package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.PuzzleSession;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.PuzzleDTO;
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

    private final PuzzleRepository puzzleRepository;
    private final PuzzleAttemptRepository puzzleAttemptRepository;
    private final PuzzleSessionRepository puzzleSessionRepository;
    private final UserRepository userRepository;
    private final EloService eloService;
    private final AITutorService aITutorService;

    @Transactional
    public Optional<PuzzleDTO> getRandomPuzzleForUser(String firebaseUid, String theme) {
        return userRepository.findById(firebaseUid)
                .flatMap(user -> {
                    autoSurrenderAbandonedFailedSessions(user);
                    return getRandomPuzzleForSession(user, theme);
                });
    }

    private void autoSurrenderAbandonedFailedSessions(User user) {
        List<PuzzleSession> abandonedSessions = puzzleSessionRepository
                .findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan(
                        user.getFirebaseUid(),
                        AUTO_SURRENDER_FAILED_ATTEMPT_THRESHOLD
                );

        abandonedSessions.forEach(this::surrenderSession);
    }

    private Optional<PuzzleDTO> getRandomPuzzleForSession(User user, String theme) {
        return puzzleRepository.findRandomPuzzleByThemeAndRating(theme, resolveMinRating(user), resolveMaxRating(user))
                .map(puzzle -> createPuzzleSession(user, puzzle));
    }

    private PuzzleDTO createPuzzleSession(User user, Puzzle puzzle) {
        PuzzleSession session = PuzzleSession.builder()
                .user(user)
                .puzzle(puzzle)
                .nextMoveIndex(1)
                .build();
        PuzzleSession savedSession = puzzleSessionRepository.save(session);
        return PuzzleDTO.from(puzzle, savedSession.getId());
    }

    private int resolveMinRating(User user) {
        return Math.max(0, resolveUserElo(user) - PUZZLE_RATING_RANGE);
    }

    private int resolveMaxRating(User user) {
        return resolveUserElo(user) + PUZZLE_RATING_RANGE;
    }

    private int resolveUserElo(User user) {
        return Optional.ofNullable(user.getEloRating()).orElse(DEFAULT_ELO);
    }

    @Transactional
    public Optional<String[]> getPuzzleHints(String firebaseUid, Long sessionId, String puzzleId) {
        if (firebaseUid == null || firebaseUid.isBlank() || sessionId == null || puzzleId == null || puzzleId.isBlank()) {
            return Optional.empty();
        }

        return findActiveSession(firebaseUid, sessionId, puzzleId)
                .map(this::loadSessionHints);
    }

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

    private EloUpdate saveCompletedAttemptAndUpdateUserElo(PuzzleSession session) {
        return saveAttemptAndUpdateUserElo(session, true);
    }

    private PuzzleSurrenderResponseDTO surrenderSession(PuzzleSession session) {
        EloUpdate eloUpdate = saveAttemptAndUpdateUserElo(session, false);
        session.setCompleted(true);
        puzzleSessionRepository.save(session);

        return new PuzzleSurrenderResponseDTO(
                true,
                eloUpdate.newElo(),
                eloUpdate.eloChange()
        );
    }

    private EloUpdate saveAttemptAndUpdateUserElo(PuzzleSession session, boolean solved) {
        User user = userRepository.findById(session.getUser().getFirebaseUid())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
        Puzzle puzzle = session.getPuzzle();
        int hintsUsed = session.getHintsUsed();
        int failedAttempts = session.getFailedAttempts();

        int currentElo = resolveUserElo(user);
        int newElo = solved
                ? eloService.calculateNewPlayerElo(currentElo, puzzle.getRating(), hintsUsed, failedAttempts)
                : eloService.calculateNewPlayerEloForFailedPuzzle(currentElo, puzzle.getRating());
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

    private record EloUpdate(int newElo, int eloChange) {
    }

    private String getOptionalNormalizedMove(Puzzle puzzle, int moveIndex) {
        return puzzle.getMoveAt(moveIndex).trim().toLowerCase();
    }

    private Optional<PuzzleSession> findActiveSession(String firebaseUid, Long sessionId, String puzzleId) {
        return puzzleSessionRepository.findByIdAndUserFirebaseUid(sessionId, firebaseUid)
                .filter(session -> !session.isCompleted())
                .filter(session -> session.getPuzzle().getId().equals(puzzleId));
    }

    private String[] loadSessionHints(PuzzleSession session) {
        session.setHintsUsed(Math.min(MAX_HINT_COUNT, session.getHintsUsed() + 1));

        if (session.getGeneratedHints() == null || session.getGeneratedHints().isBlank()) {
            String[] hints = generateHints(session.getPuzzle());
            session.setGeneratedHints(String.join("\n", hints));
            puzzleSessionRepository.save(session);
            return hints;
        }

        puzzleSessionRepository.save(session);
        return Arrays.stream(session.getGeneratedHints().split("\\R"))
                .filter(hint -> !hint.isBlank())
                .toArray(String[]::new);
    }

    private String[] generateHints(Puzzle puzzle) {
        List<String> solution = Arrays.stream(puzzle.getMoves().trim().split("\\s+"))
                .filter(move -> !move.isBlank())
                .toList();

        List<String> themes = Arrays.stream(Optional.ofNullable(puzzle.getThemes()).orElse("").split("\\s+"))
                .filter(theme -> !theme.isBlank())
                .toList();

        return aITutorService.getHints(puzzle.getFen(), solution, themes);
    }


}
