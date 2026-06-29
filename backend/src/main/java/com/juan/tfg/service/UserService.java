package com.juan.tfg.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.EloHistoryPointDTO;
import com.juan.tfg.model.dto.UserLeaderboardEntryDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.UserRepository;
import com.juan.tfg.service.exception.DuplicateUsernameException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int RANDOM_USERNAME_SUFFIX_DIGITS = 6;
    private static final int RANDOM_USERNAME_SUFFIX_BOUND = 1_000_000;
    private static final int MAX_USERNAME_GENERATION_ATTEMPTS = 20;
    private static final SecureRandom USERNAME_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final FirebaseApp firebaseApp;
    private final PuzzleAttemptRepository puzzleAttemptRepository;

    /**
     * Loads a user by Firebase UID or creates one from Firebase Auth when missing.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the existing or newly created user.
     */
    @Transactional
    public User getOrCreateUser(String firebaseUid) {
        return userRepository.findById(firebaseUid).orElseGet(() -> createUserFromFirebase(firebaseUid));
    }

    /**
     * Loads a user by Firebase token or creates one using the token claims.
     *
     * @param firebaseToken the verified Firebase token.
     * @return the existing or newly created user.
     */
    @Transactional
    public User getOrCreateUser(FirebaseToken firebaseToken) {
        Optional<User> existingUser = userRepository.findById(firebaseToken.getUid());

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        String firebaseEmail = firebaseToken.getEmail();
        String email = resolveEmail(firebaseEmail, firebaseToken.getUid());
        String username = resolveUniqueUsername(firebaseToken.getName(), firebaseEmail);
        return saveNewUser(firebaseToken.getUid(), email, username);
    }

    /**
     * Returns the user's Elo history in chronological order.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the user's Elo history entries.
     */
    @Transactional(readOnly = true)
    public List<EloHistoryPointDTO> getEloHistory(String firebaseUid) {
        return puzzleAttemptRepository.findEloHistoryByUserId(firebaseUid).stream()
                .map(this::toEloHistoryPointDTO)
                .toList();
    }

    /**
     * Validates and updates a user's username.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param requestedUsername the requested username value.
     * @return the updated user.
     * @throws DuplicateUsernameException if another user already owns the username.
     * @throws IllegalArgumentException if the requested username is invalid.
     */
    @Transactional
    public User updateUsername(String firebaseUid, String requestedUsername) {
        User user = getOrCreateUser(firebaseUid);
        String username = validateRequestedUsername(requestedUsername);

        if (username.equals(user.getUsername())) {
            return user;
        }

        if (userRepository.existsByUsernameAndFirebaseUidNot(username, firebaseUid)) {
            throw new DuplicateUsernameException("Username is already in use.");
        }

        user.setUsername(username);
        return userRepository.save(user);
    }

    /**
     * Returns all users as leaderboard entries ordered by current Elo.
     *
     * @param currentFirebaseUid the Firebase UID of the requesting user.
     * @return leaderboard entries with daily rank movement and current-user marking.
     */
    @Transactional(readOnly = true)
    public List<UserLeaderboardEntryDTO> getUsersOrderedByEloRating(String currentFirebaseUid) {
        List<User> currentLeaderboard = userRepository.findAllByOrderByEloRatingDescUsernameAsc();
        Map<String, Integer> dailyEloChanges = getDailyEloChanges();
        Map<String, Integer> startOfDayRanks = getStartOfDayRanks(currentLeaderboard, dailyEloChanges);

        return IntStream.range(0, currentLeaderboard.size())
                .mapToObj(index -> toLeaderboardEntryDTO(
                        currentLeaderboard.get(index),
                        startOfDayRanks.get(currentLeaderboard.get(index).getFirebaseUid()) - (index + 1),
                        currentFirebaseUid
                ))
                .toList();
    }

    /**
     * Loads the total Elo changes produced since the start of the current day.
     *
     * @return daily Elo changes keyed by Firebase UID.
     */
    private Map<String, Integer> getDailyEloChanges() {
        return puzzleAttemptRepository.findDailyEloChangesSince(LocalDate.now().atStartOfDay()).stream()
                .collect(Collectors.toMap(
                        PuzzleAttemptRepository.UserDailyEloChange::getFirebaseUid,
                        dailyChange -> Math.toIntExact(dailyChange.getEloChange())
                ));
    }

    /**
     * Reconstructs leaderboard ranks as they were at the start of the current day.
     *
     * @param currentLeaderboard users ordered by current leaderboard position.
     * @param dailyEloChanges daily Elo changes keyed by Firebase UID.
     * @return start-of-day ranks keyed by Firebase UID.
     */
    private Map<String, Integer> getStartOfDayRanks(List<User> currentLeaderboard, Map<String, Integer> dailyEloChanges) {
        List<User> startOfDayLeaderboard = currentLeaderboard.stream()
                .sorted(Comparator
                        .comparingInt((User user) -> getStartOfDayElo(user, dailyEloChanges))
                        .reversed()
                        .thenComparing(User::getUsername))
                .toList();

        return IntStream.range(0, startOfDayLeaderboard.size())
                .boxed()
                .collect(Collectors.toMap(
                        index -> startOfDayLeaderboard.get(index).getFirebaseUid(),
                        index -> index + 1
                ));
    }

    /**
     * Calculates a user's Elo at the start of the day.
     *
     * @param user the user whose historical Elo should be calculated.
     * @param dailyEloChanges daily Elo changes keyed by Firebase UID.
     * @return the user's reconstructed start-of-day Elo.
     */
    private int getStartOfDayElo(User user, Map<String, Integer> dailyEloChanges) {
        return resolveEloRating(user) - dailyEloChanges.getOrDefault(user.getFirebaseUid(), 0);
    }

    /**
     * Resolves a user's Elo rating, using the default rating when the stored value is null.
     *
     * @param user the user whose Elo should be read.
     * @return the resolved Elo rating.
     */
    private int resolveEloRating(User user) {
        return Optional.ofNullable(user.getEloRating()).orElse(1000);
    }

    /**
     * Converts a user into a leaderboard response entry.
     *
     * @param user the user to convert.
     * @param dailyRankChange the user's rank movement since the start of the day.
     * @param currentFirebaseUid the Firebase UID of the requesting user.
     * @return the leaderboard entry DTO.
     */
    private UserLeaderboardEntryDTO toLeaderboardEntryDTO(User user, int dailyRankChange, String currentFirebaseUid) {
        return new UserLeaderboardEntryDTO(
                user.getUsername(),
                user.getEloRating(),
                dailyRankChange,
                Objects.equals(user.getFirebaseUid(), currentFirebaseUid)
        );
    }

    /**
     * Converts a puzzle attempt into an Elo history point.
     *
     * @param puzzleAttempt the attempt to convert.
     * @return the Elo history point DTO.
     */
    private EloHistoryPointDTO toEloHistoryPointDTO(PuzzleAttempt puzzleAttempt) {
        return new EloHistoryPointDTO(
                puzzleAttempt.getId(),
                puzzleAttempt.getAttemptDate(),
                puzzleAttempt.getPuzzle().getRating(),
                puzzleAttempt.getEloChange(),
                puzzleAttempt.getResultingElo()
        );
    }

    /**
     * Counts all puzzle attempts made by a user.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the number of puzzle attempts.
     */
    @Transactional(readOnly = true)
    public long countPuzzleAttempts(String firebaseUid) {
        return puzzleAttemptRepository.countByFirebaseUid(firebaseUid);
    }

    /**
     * Counts successfully solved puzzles for a user.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the number of successful puzzle attempts.
     */
    @Transactional(readOnly = true)
    public long countSolvedPuzzles(String firebaseUid) {
        return puzzleAttemptRepository.countSuccessfulByFirebaseUid(firebaseUid);
    }

    /**
     * Creates a user by loading the authoritative profile from Firebase Auth.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the newly saved user.
     */
    private User createUserFromFirebase(String firebaseUid) {
        try {
            UserRecord firebaseUser = FirebaseAuth.getInstance(firebaseApp).getUser(firebaseUid);
            String firebaseEmail = firebaseUser.getEmail();
            String email = resolveEmail(firebaseEmail, firebaseUid);
            String username = resolveUniqueUsername(firebaseUser.getDisplayName(), firebaseEmail);
            return saveNewUser(firebaseUser.getUid(), email, username);
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Unable to verify Firebase user.", e);
        }
    }

    /**
     * Persists a new user record in the application database.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param email the normalized email address.
     * @param username the unique username.
     * @return the saved user entity.
     */
    private User saveNewUser(String firebaseUid, String email, String username) {
        User newUser = User.builder()
                .firebaseUid(firebaseUid)
                .email(email)
                .username(username)
                .build();

        User savedUser = userRepository.save(newUser);
        logger.info("Registered Firebase user in PostgreSQL with UID {}.", firebaseUid);
        return savedUser;
    }

    /**
     * Resolves the stored user email, falling back to a local Firebase-derived address.
     *
     * @param email the email from Firebase, when present.
     * @param firebaseUid the Firebase user identifier.
     * @return the normalized email value.
     */
    private String resolveEmail(String email, String firebaseUid) {
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase(Locale.ROOT);
        }

        return firebaseUid + "@firebase.local";
    }

    /**
     * Builds a unique username from display name or email data.
     *
     * @param displayName the Firebase display name.
     * @param email the Firebase email address.
     * @return a unique username value.
     */
    private String resolveUniqueUsername(String displayName, String email) {
        String baseUsername = resolveBaseUsername(displayName, email);
        String username = truncate(baseUsername, MAX_USERNAME_LENGTH);

        if (!userRepository.existsByUsername(username)) {
            return username;
        }

        return resolveRandomizedUsername(baseUsername);
    }

    /**
     * Resolves the base username before uniqueness suffixes are added.
     *
     * @param displayName the Firebase display name.
     * @param email the Firebase email address.
     * @return the normalized base username.
     */
    private String resolveBaseUsername(String displayName, String email) {
        String baseUsername = normalizeUsername(displayName);

        if (baseUsername.isBlank() && email != null && email.contains("@")) {
            baseUsername = normalizeUsername(email.substring(0, email.indexOf('@')));
        }

        if (baseUsername.isBlank()) {
            return "user";
        }

        return baseUsername;
    }

    /**
     * Adds a random numeric suffix until a unique username is found.
     *
     * @param baseUsername the preferred base username.
     * @return a unique randomized username.
     * @throws IllegalStateException if a unique username cannot be generated.
     */
    private String resolveRandomizedUsername(String baseUsername) {
        for (int attempt = 0; attempt < MAX_USERNAME_GENERATION_ATTEMPTS; attempt++) {
            String suffix = "-" + randomNumericSuffix();
            String candidate = truncate(baseUsername, MAX_USERNAME_LENGTH - suffix.length()) + suffix;

            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }

        throw new IllegalStateException("Unable to generate a unique username.");
    }

    /**
     * Generates a zero-padded numeric username suffix.
     *
     * @return the random numeric suffix.
     */
    private String randomNumericSuffix() {
        return String.format(Locale.ROOT, "%0" + RANDOM_USERNAME_SUFFIX_DIGITS + "d", USERNAME_RANDOM.nextInt(RANDOM_USERNAME_SUFFIX_BOUND));
    }

    /**
     * Normalizes a raw username-like value into the allowed username format.
     *
     * @param value the raw value to normalize.
     * @return the normalized username, or an empty string when the value is null.
     */
    private String normalizeUsername(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Normalizes and validates a requested username.
     *
     * @param requestedUsername the raw requested username.
     * @return the normalized username.
     * @throws IllegalArgumentException if the username is blank or too long.
     */
    private String validateRequestedUsername(String requestedUsername) {
        String username = normalizeUsername(requestedUsername);

        if (username.isBlank()) {
            throw new IllegalArgumentException("Username must include at least one letter or number.");
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must be 50 characters or fewer.");
        }

        return username;
    }

    /**
     * Truncates a string to the requested maximum length.
     *
     * @param value the value to truncate.
     * @param maxLength the maximum allowed length.
     * @return the original value or a truncated prefix.
     */
    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
