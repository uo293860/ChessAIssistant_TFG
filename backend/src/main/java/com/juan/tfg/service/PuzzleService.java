package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.PuzzleSession;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleHintResponseDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.model.dto.PuzzleSurrenderResponseDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.PuzzleRepository;
import com.juan.tfg.repository.PuzzleSessionRepository;
import com.juan.tfg.repository.UserRepository;
import com.juan.tfg.service.aitutor.AITutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PuzzleService {

    private static final int DEFAULT_ELO = 1000;
    private static final int PUZZLE_RATING_RANGE = 100;
    private static final int MAX_HINT_COUNT = 3;
    private static final int AUTO_SURRENDER_FAILED_ATTEMPT_THRESHOLD = 0;
    private static final int CLEAN_SOLVE_HINT_COUNT = 0;
    private static final int CLEAN_SOLVE_FAILED_ATTEMPT_COUNT = 0;
    private static final int SINGLE_HINT_COUNT = 1;

    private final PuzzleRepository puzzleRepository;
    private final PuzzleAttemptRepository puzzleAttemptRepository;
    private final PuzzleSessionRepository puzzleSessionRepository;
    private final UserRepository userRepository;
    private final EloService eloService;
    private final AITutorService aITutorService;
    private final PuzzleThemeCatalog puzzleThemeCatalog;

    /**
     * Starts a new puzzle session for a user using the user's Elo and optional theme.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param themeId the optional puzzle theme identifier.
     * @return the created puzzle DTO, or an empty result when the user or puzzle cannot be resolved.
     */
    @Transactional
    public Optional<PuzzleDTO> getRandomPuzzleForUser(String firebaseUid, String themeId) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return Optional.empty();
        }

        Optional<String> selectedThemeId = puzzleThemeCatalog.resolveSelectedThemeId(themeId);

        return userRepository.findById(firebaseUid)
                .flatMap(user -> {
                    autoSurrenderAbandonedFailedSessions(user);
                    return getRandomPuzzleForSession(user, selectedThemeId);
                });
    }

    /**
     * Surrenders incomplete sessions that already contain failed attempts before starting a new puzzle.
     *
     * @param user the user whose abandoned sessions should be closed.
     */
    private void autoSurrenderAbandonedFailedSessions(User user) {
        List<PuzzleSession> abandonedSessions = puzzleSessionRepository
                .findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan(
                        user.getFirebaseUid(),
                        AUTO_SURRENDER_FAILED_ATTEMPT_THRESHOLD
                );

        abandonedSessions.forEach(this::surrenderSession);
    }

    /**
     * Selects a random puzzle for a user and creates a session for it.
     *
     * @param user the user who will solve the puzzle.
     * @param themeId the optional theme filter.
     * @return the created puzzle DTO, or an empty result when no puzzle matches.
     */
    private Optional<PuzzleDTO> getRandomPuzzleForSession(User user, Optional<String> themeId) {
        Optional<Puzzle> selectedPuzzle = themeId
                .map(selectedThemeId -> findRandomPuzzleByThemeWithRatingFallback(user, selectedThemeId))
                .orElseGet(() -> puzzleRepository.findRandomPuzzleByRating(resolveMinRating(user), resolveMaxRating(user)));

        return selectedPuzzle
                .map(puzzle -> createPuzzleSession(user, puzzle));
    }

    /**
     * Finds a themed random puzzle, first inside the user's rating range and then without rating limits.
     *
     * @param user the user whose rating range is preferred.
     * @param themeId the puzzle theme identifier.
     * @return a matching themed puzzle, or an empty result when none exists.
     */
    private Optional<Puzzle> findRandomPuzzleByThemeWithRatingFallback(User user, String themeId) {
        return puzzleRepository.findRandomPuzzleByThemeAndRating(themeId, resolveMinRating(user), resolveMaxRating(user))
                .or(() -> puzzleRepository.findRandomPuzzleByTheme(themeId));
    }

    /**
     * Starts a retry session for one random puzzle the user previously failed.
     *
     * @param firebaseUid the Firebase user identifier.
     * @return the retry puzzle DTO, or an empty result when no failed attempts exist.
     */
    @Transactional
    public Optional<PuzzleDTO> getRandomFailedPuzzleForUser(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return Optional.empty();
        }

        return puzzleAttemptRepository.findRandomFailedAttempt(firebaseUid)
                .map(attempt -> createPuzzleSession(attempt.getUser(), attempt.getPuzzle(), attempt));
    }

    /**
     * Creates a standard puzzle session for a user.
     *
     * @param user the user who owns the session.
     * @param puzzle the puzzle assigned to the session.
     * @return the created puzzle DTO.
     */
    private PuzzleDTO createPuzzleSession(User user, Puzzle puzzle) {
        return createPuzzleSession(user, puzzle, null);
    }

    /**
     * Creates a puzzle session, optionally linked to a failed attempt being retried.
     *
     * @param user the user who owns the session.
     * @param puzzle the puzzle assigned to the session.
     * @param retryAttempt the failed attempt being retried, or null for a normal session.
     * @return the created puzzle DTO.
     */
    private PuzzleDTO createPuzzleSession(User user, Puzzle puzzle, PuzzleAttempt retryAttempt) {
        PuzzleSession session = PuzzleSession.builder()
                .user(user)
                .puzzle(puzzle)
                .retryAttempt(retryAttempt)
                .nextMoveIndex(1)
                .build();
        PuzzleSession savedSession = puzzleSessionRepository.save(session);
        return PuzzleDTO.from(puzzle, savedSession.getId(), calculateHintEloPenalty(resolveUserElo(user), puzzle.getRating()));
    }

    /**
     * Estimates how many Elo points one hint costs compared with a clean solve.
     *
     * @param userElo the user's current Elo.
     * @param puzzleElo the puzzle rating.
     * @return the non-negative Elo penalty estimate.
     */
    private int calculateHintEloPenalty(int userElo, int puzzleElo) {
        int cleanSolveElo = eloService.calculateNewPlayerElo(
                userElo,
                puzzleElo,
                true,
                CLEAN_SOLVE_HINT_COUNT,
                CLEAN_SOLVE_FAILED_ATTEMPT_COUNT
        );
        int singleHintElo = eloService.calculateNewPlayerElo(
                userElo,
                puzzleElo,
                true,
                SINGLE_HINT_COUNT,
                CLEAN_SOLVE_FAILED_ATTEMPT_COUNT
        );

        return Math.max(0, cleanSolveElo - singleHintElo);
    }

    /**
     * Resolves the minimum puzzle rating that should be selected for a user.
     *
     * @param user the user whose Elo determines the range.
     * @return the inclusive minimum puzzle rating.
     */
    private int resolveMinRating(User user) {
        return Math.max(0, resolveUserElo(user) - PUZZLE_RATING_RANGE);
    }

    /**
     * Resolves the maximum puzzle rating that should be selected for a user.
     *
     * @param user the user whose Elo determines the range.
     * @return the inclusive maximum puzzle rating.
     */
    private int resolveMaxRating(User user) {
        return resolveUserElo(user) + PUZZLE_RATING_RANGE;
    }

    /**
     * Resolves a user's Elo, using the default rating when the stored value is null.
     *
     * @param user the user whose Elo should be read.
     * @return the resolved Elo rating.
     */
    private int resolveUserElo(User user) {
        return Optional.ofNullable(user.getEloRating()).orElse(DEFAULT_ELO);
    }

    /**
     * Reveals the next hint for an active puzzle session.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param sessionId the active session identifier.
     * @param puzzleId the puzzle identifier.
     * @return the next hint response, or an empty result when the request is invalid.
     */
    @Transactional
    public Optional<PuzzleHintResponseDTO> getPuzzleHint(String firebaseUid, Long sessionId, String puzzleId) {
        if (firebaseUid == null || firebaseUid.isBlank() || sessionId == null || puzzleId == null || puzzleId.isBlank()) {
            return Optional.empty();
        }

        return findActiveSession(firebaseUid, sessionId, puzzleId)
                .flatMap(this::revealNextSessionHint);
    }

    /**
     * Verifies a submitted move for an active puzzle session.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param sessionId the active session identifier.
     * @param puzzleId the puzzle identifier.
     * @param move the submitted move.
     * @return the verification response, or an empty result when the request is invalid.
     */
    @Transactional
    public Optional<PuzzleMoveVerificationResponseDTO> verifyMove(
            String firebaseUid,
            Long sessionId,
            String puzzleId,
            String move
    ) {
        if (firebaseUid == null || firebaseUid.isBlank() || sessionId == null || puzzleId == null || puzzleId.isBlank()
                || move == null || move.isBlank()) {
            return Optional.empty();
        }

        return findActiveSession(firebaseUid, sessionId, puzzleId)
                .map(session -> buildVerificationResponse(session, move));
    }

    /**
     * Surrenders an active puzzle session and applies the corresponding Elo update.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param sessionId the active session identifier.
     * @param puzzleId the puzzle identifier.
     * @return the surrender response, or an empty result when the request is invalid.
     */
    @Transactional
    public Optional<PuzzleSurrenderResponseDTO> surrenderPuzzle(
            String firebaseUid,
            Long sessionId,
            String puzzleId
    ) {
        if (firebaseUid == null || firebaseUid.isBlank() || sessionId == null || puzzleId == null || puzzleId.isBlank()) {
            return Optional.empty();
        }

        return findActiveSession(firebaseUid, sessionId, puzzleId)
                .map(this::surrenderSession);
    }

    /**
     * Builds the verification response and updates the session state for a submitted move.
     *
     * @param session the active puzzle session.
     * @param move the submitted move.
     * @return the move verification response.
     */
    private PuzzleMoveVerificationResponseDTO buildVerificationResponse(
            PuzzleSession session,
            String move
    ) {
        Puzzle puzzle = session.getPuzzle();
        int moveIndex = session.getNextMoveIndex();
        String normalizedMove = move.trim().toLowerCase();
        String expectedMove = puzzle.getMoveAt(moveIndex).trim().toLowerCase();

        if (expectedMove.isBlank()) {
            return new PuzzleMoveVerificationResponseDTO(
                    false,
                    "",
                    moveIndex,
                    false,
                    null,
                    null
            );
        }

        boolean correct = expectedMove.equals(normalizedMove);

        if (!correct) {
            session.setFailedAttempts(session.getFailedAttempts() + 1);
            puzzleSessionRepository.save(session);
            return new PuzzleMoveVerificationResponseDTO(
                    false,
                    "",
                    moveIndex,
                    false,
                    null,
                    null
            );
        }

        String opponentMove = getOptionalNormalizedMove(puzzle, moveIndex + 1);
        int nextMoveIndex = opponentMove.isBlank() ? moveIndex + 1 : moveIndex + 2;
        boolean puzzleCompleted = moveIndex >= puzzle.getMoveCount() - 1;
        EloUpdate eloUpdate = null;

        session.setNextMoveIndex(nextMoveIndex);

        if (puzzleCompleted) {
            eloUpdate = saveCompletedAttemptAndUpdateUserElo(session);
            session.setCompleted(true);
        }

        puzzleSessionRepository.save(session);

        return new PuzzleMoveVerificationResponseDTO(
                true,
                opponentMove,
                nextMoveIndex,
                puzzleCompleted,
                eloUpdate == null ? null : eloUpdate.newElo(),
                eloUpdate == null ? null : eloUpdate.eloChange()
        );
    }

    /**
     * Persists a successful attempt and updates the user's Elo.
     *
     * @param session the completed puzzle session.
     * @return the resulting Elo update.
     */
    private EloUpdate saveCompletedAttemptAndUpdateUserElo(PuzzleSession session) {
        return saveAttemptAndUpdateUserElo(session, true);
    }

    /**
     * Surrenders a session, stores the failed attempt, and returns the remaining solution.
     *
     * @param session the active puzzle session to surrender.
     * @return the surrender response.
     */
    private PuzzleSurrenderResponseDTO surrenderSession(PuzzleSession session) {
        EloUpdate eloUpdate = saveAttemptAndUpdateUserElo(session, false);
        session.setCompleted(true);
        puzzleSessionRepository.save(session);

        return new PuzzleSurrenderResponseDTO(
                true,
                eloUpdate.newElo(),
                eloUpdate.eloChange(),
                getSolutionMoves(session.getPuzzle())
        );
    }

    /**
     * Saves the final attempt for a session and updates Elo when the session is not a retry.
     *
     * @param session the completed or surrendered puzzle session.
     * @param solved whether the user solved the puzzle.
     * @return the resulting Elo update.
     */
    private EloUpdate saveAttemptAndUpdateUserElo(PuzzleSession session, boolean solved) {
        User user = userRepository.findById(session.getUser().getFirebaseUid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
        Puzzle puzzle = session.getPuzzle();
        int hintsUsed = session.getHintsUsed();
        int failedAttempts = session.getFailedAttempts();

        int currentElo = resolveUserElo(user);
        if (session.getRetryAttempt() != null) {
            return updateRetriedAttempt(session, solved, user, currentElo);
        }

        int newElo = eloService.calculateNewPlayerElo(currentElo, puzzle.getRating(), solved, hintsUsed, failedAttempts);
        int eloChange = newElo - currentElo;

        PuzzleAttempt puzzleAttempt = PuzzleAttempt.builder()
                .user(user)
                .puzzle(puzzle)
                .isSuccessful(solved && failedAttempts == 0)
                .hintsUsed(hintsUsed)
                .failedAttempts(failedAttempts)
                .eloChange(eloChange)
                .resultingElo(newElo)
                .build();

        puzzleAttemptRepository.save(puzzleAttempt);
        user.setEloRating(newElo);
        userRepository.save(user);

        return new EloUpdate(newElo, eloChange);
    }

    /**
     * Updates the original failed attempt when a retry session is solved without changing Elo.
     *
     * @param session the retry session.
     * @param solved whether the retry was solved.
     * @param user the user who owns the retry.
     * @param currentElo the user's current Elo.
     * @return an Elo update with no rating change.
     */
    private EloUpdate updateRetriedAttempt(PuzzleSession session, boolean solved, User user, int currentElo) {
        PuzzleAttempt retryAttempt = session.getRetryAttempt();

        if (!retryAttempt.getUser().getFirebaseUid().equals(user.getFirebaseUid())
                || !retryAttempt.getPuzzle().getId().equals(session.getPuzzle().getId())) {
            throw new IllegalStateException("Retry session does not match the failed puzzle attempt.");
        }

        if (solved) {
            retryAttempt.setIsSuccessful(true);
            puzzleAttemptRepository.save(retryAttempt);
        }

        return new EloUpdate(currentElo, 0);
    }

    /**
     * Holds the result of an Elo update.
     *
     * @param newElo the user's Elo after the update.
     * @param eloChange the Elo difference produced by the update.
     */
    private record EloUpdate(int newElo, int eloChange) {
    }

    /**
     * Returns a normalized move from a puzzle solution when it exists.
     *
     * @param puzzle the puzzle containing the move list.
     * @param moveIndex the zero-based move index.
     * @return the normalized move, or an empty string when the move is not available.
     */
    private String getOptionalNormalizedMove(Puzzle puzzle, int moveIndex) {
        return puzzle.getMoveAt(moveIndex).trim().toLowerCase();
    }

    /**
     * Finds an active session that belongs to the user and matches the requested puzzle.
     *
     * @param firebaseUid the Firebase user identifier.
     * @param sessionId the session identifier.
     * @param puzzleId the puzzle identifier.
     * @return the matching active session, or an empty result when no valid session exists.
     */
    private Optional<PuzzleSession> findActiveSession(String firebaseUid, Long sessionId, String puzzleId) {
        return puzzleSessionRepository.findByIdAndUserFirebaseUid(sessionId, firebaseUid)
                .filter(session -> !session.isCompleted())
                .filter(session -> session.getPuzzle().getId().equals(puzzleId));
    }

    /**
     * Reveals and persists the next unrevealed hint for a session.
     *
     * @param session the active puzzle session.
     * @return the next hint response, or an empty result when all hints are already revealed.
     */
    private Optional<PuzzleHintResponseDTO> revealNextSessionHint(PuzzleSession session) {
        List<String> hints = loadSessionHints(session);
        int availableHintCount = Math.min(hints.size(), MAX_HINT_COUNT);
        int revealedHintCount = Math.max(0, session.getHintsUsed());

        if (revealedHintCount >= availableHintCount) {
            return Optional.empty();
        }

        String hint = hints.get(revealedHintCount);
        int nextHintNumber = revealedHintCount + 1;
        session.setHintsUsed(nextHintNumber);
        puzzleSessionRepository.save(session);

        return Optional.of(new PuzzleHintResponseDTO(
                hint,
                nextHintNumber,
                MAX_HINT_COUNT,
                nextHintNumber >= availableHintCount
        ));
    }

    /**
     * Loads cached hints for a session or generates and stores them when missing.
     *
     * @param session the puzzle session whose hints should be loaded.
     * @return the normalized hint list.
     */
    private List<String> loadSessionHints(PuzzleSession session) {
        if (session.getGeneratedHints() == null || session.getGeneratedHints().isBlank()) {
            List<String> hints = normalizeHints(generateHints(session.getPuzzle()));
            session.setGeneratedHints(String.join("\n", hints));
            return hints;
        }

        return parseStoredHints(session.getGeneratedHints());
    }

    /**
     * Normalizes generated hint text into a bounded non-empty list.
     *
     * @param hints the raw hint array returned by the AI tutor.
     * @return a normalized hint list.
     */
    private List<String> normalizeHints(String[] hints) {
        if (hints == null) {
            return List.of("No hint is available for this position.");
        }

        List<String> normalizedHints = Arrays.stream(hints)
                .filter(hint -> hint != null && !hint.isBlank())
                .map(String::trim)
                .limit(MAX_HINT_COUNT)
                .toList();

        return normalizedHints.isEmpty()
                ? List.of("No hint is available for this position.")
                : normalizedHints;
    }

    /**
     * Parses hints previously stored in a session.
     *
     * @param generatedHints the stored newline-separated hints.
     * @return a normalized hint list limited to the maximum hint count.
     */
    private List<String> parseStoredHints(String generatedHints) {
        return Arrays.stream(generatedHints.split("\\R"))
                .filter(hint -> !hint.isBlank())
                .map(String::trim)
                .limit(MAX_HINT_COUNT)
                .toList();
    }

    /**
     * Generates puzzle hints by sending the position, solution, and themes to the AI tutor.
     *
     * @param puzzle the puzzle that needs hints.
     * @return the raw hints returned by the AI tutor.
     */
    private String[] generateHints(Puzzle puzzle) {
        List<String> solution = getPuzzleMoves(puzzle);

        List<String> themes = Arrays.stream(Optional.ofNullable(puzzle.getThemes()).orElse("").split("\\s+"))
                .filter(theme -> !theme.isBlank())
                .toList();

        return aITutorService.getHints(puzzle.getFen(), solution, themes, puzzle.getInitialMove());
    }

    /**
     * Returns the solution moves the user should see after surrendering.
     *
     * @param puzzle the surrendered puzzle.
     * @return the solution moves after the initial move.
     */
    private List<String> getSolutionMoves(Puzzle puzzle) {
        List<String> moves = getPuzzleMoves(puzzle);

        if (moves.size() <= 1) {
            return List.of();
        }

        return moves.subList(1, moves.size());
    }

    /**
     * Parses the puzzle's stored move sequence into individual moves.
     *
     * @param puzzle the puzzle containing a whitespace-separated move list.
     * @return the parsed move list without blank entries.
     */
    private List<String> getPuzzleMoves(Puzzle puzzle) {
        return Arrays.stream(Optional.ofNullable(puzzle.getMoves()).orElse("").trim().split("\\s+"))
                .filter(move -> !move.isBlank())
                .toList();
    }

}
