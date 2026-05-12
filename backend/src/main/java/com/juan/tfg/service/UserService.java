package com.juan.tfg.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.EloHistoryPointDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_USERNAME_LENGTH = 50;

    private final UserRepository userRepository;
    private final PuzzleAttemptRepository puzzleAttemptRepository;
    private final FirebaseApp firebaseApp;

    @Transactional
    public User getOrCreateUser(String firebaseUid) {
        return userRepository.findById(firebaseUid).orElseGet(() -> createUserFromFirebase(firebaseUid));
    }

    @Transactional
    public User getOrCreateUser(FirebaseToken firebaseToken) {
        Optional<User> existingUser = userRepository.findById(firebaseToken.getUid());

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        String email = resolveEmail(firebaseToken.getEmail(), firebaseToken.getUid());
        String username = resolveUniqueUsername(firebaseToken.getName(), email, firebaseToken.getUid());
        return saveNewUser(firebaseToken.getUid(), email, username);
    }

    @Transactional(readOnly = true)
    public List<EloHistoryPointDTO> getEloHistory(String firebaseUid) {
        return puzzleAttemptRepository.findSuccessfulEloHistoryByUserId(firebaseUid).stream()
                .map(this::toEloHistoryPointDTO)
                .toList();
    }

    private EloHistoryPointDTO toEloHistoryPointDTO(PuzzleAttempt puzzleAttempt) {
        return new EloHistoryPointDTO(
                puzzleAttempt.getId(),
                puzzleAttempt.getAttemptDate(),
                puzzleAttempt.getPuzzle().getRating(),
                puzzleAttempt.getEloChange(),
                puzzleAttempt.getResultingElo()
        );
    }

    private User createUserFromFirebase(String firebaseUid) {
        try {
            UserRecord firebaseUser = FirebaseAuth.getInstance(firebaseApp).getUser(firebaseUid);
            String email = resolveEmail(firebaseUser.getEmail(), firebaseUid);
            String username = resolveUniqueUsername(firebaseUser.getDisplayName(), email, firebaseUid);
            return saveNewUser(firebaseUser.getUid(), email, username);
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Unable to verify Firebase user.", e);
        }
    }

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

    private String resolveEmail(String email, String firebaseUid) {
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase(Locale.ROOT);
        }

        return firebaseUid + "@firebase.local";
    }

    private String resolveUniqueUsername(String displayName, String email, String firebaseUid) {
        String baseUsername = normalizeUsername(displayName);

        if (baseUsername.isBlank()) {
            baseUsername = normalizeUsername(email.substring(0, email.indexOf('@')));
        }

        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        String username = truncate(baseUsername, MAX_USERNAME_LENGTH);

        if (!userRepository.existsByUsername(username)) {
            return username;
        }

        String suffix = "-" + firebaseUid.substring(0, Math.min(firebaseUid.length(), 8)).toLowerCase(Locale.ROOT);
        return truncate(baseUsername, MAX_USERNAME_LENGTH - suffix.length()) + suffix;
    }

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

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
