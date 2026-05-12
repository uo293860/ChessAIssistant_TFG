package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.PuzzleRepository;
import com.juan.tfg.repository.UserRepository;
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
    private static final int PUZZLE_RATING_RANGE = 50;

    private final PuzzleRepository puzzleRepository;
    private final PuzzleAttemptRepository puzzleAttemptRepository;
    private final UserRepository userRepository;
    private final EloService eloService;
    private final AITutorService aITutorService;

    @Transactional(readOnly = true)
    public Optional<PuzzleDTO> getRandomPuzzleForUser(String firebaseUid, String theme) {
        return userRepository.findById(firebaseUid)
                .flatMap(user -> getRandomPuzzle(theme, resolveMinRating(user), resolveMaxRating(user)));
    }

    private Optional<PuzzleDTO> getRandomPuzzle(String theme, int minRating, int maxRating) {
        return puzzleRepository.findRandomPuzzleByThemeAndRating(theme, minRating, maxRating)
                .map(PuzzleDTO::from);
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

    public Optional<String[]> getPuzzleHints(String puzzleId) {
        if (puzzleId == null || puzzleId.isBlank()) {
            return Optional.empty();
        }

        return puzzleRepository.findById(puzzleId)
                .map(this::generateHints);
    }

    @Transactional
    public Optional<PuzzleMoveVerificationResponseDTO> verifyMove(
            String firebaseUid,
            String puzzleId,
            String move,
            int moveIndex,
            int hintsUsed,
            int failedAttempts
    ) {
        if (puzzleId == null || puzzleId.isBlank() || move == null || move.isBlank() || moveIndex < 1) {
            return Optional.empty();
        }

        return puzzleRepository.findById(puzzleId)
                .map(puzzle -> buildVerificationResponse(
                        firebaseUid,
                        puzzle,
                        move,
                        moveIndex,
                        Math.max(0, hintsUsed),
                        Math.max(0, failedAttempts)
                ));
    }

    private PuzzleMoveVerificationResponseDTO buildVerificationResponse(
            String firebaseUid,
            Puzzle puzzle,
            String move,
            int moveIndex,
            int hintsUsed,
            int failedAttempts
    ) {
        String normalizedMove = move.trim().toLowerCase();
        String expectedMove = puzzle.getMoveAt(moveIndex).trim().toLowerCase();
        String opponentMove = getOptionalNormalizedMove(puzzle, moveIndex + 1);
        int nextMoveIndex = opponentMove.isBlank() ? moveIndex + 1 : moveIndex + 2;
        boolean puzzleCompleted = moveIndex >= puzzle.getMoveCount() - 1;

        if (expectedMove.isBlank()) {
            return new PuzzleMoveVerificationResponseDTO(
                    false,
                    "",
                    moveIndex,
                    false,
                    null
            );
        }

        boolean correct = expectedMove.equals(normalizedMove);
        Integer newElo = correct && puzzleCompleted
                ? saveSolvedAttemptAndUpdateUserElo(firebaseUid, puzzle, hintsUsed, failedAttempts)
                : null;

        return new PuzzleMoveVerificationResponseDTO(
                correct,
                opponentMove,
                nextMoveIndex,
                puzzleCompleted,
                newElo
        );
    }

    private int saveSolvedAttemptAndUpdateUserElo(String firebaseUid, Puzzle puzzle, int hintsUsed, int failedAttempts) {
        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));

        int currentElo = resolveUserElo(user);
        int newElo = eloService.calculateNewPlayerElo(currentElo, puzzle.getRating(), hintsUsed, failedAttempts);
        int eloChange = newElo - currentElo;

        PuzzleAttempt puzzleAttempt = PuzzleAttempt.builder()
                .user(user)
                .puzzle(puzzle)
                .isSuccessful(true)
                .hintsUsed(hintsUsed)
                .failedAttempts(failedAttempts)
                .eloChange(eloChange)
                .resultingElo(newElo)
                .build();

        puzzleAttemptRepository.save(puzzleAttempt);
        user.setEloRating(newElo);
        userRepository.save(user);

        return newElo;
    }

    private String getOptionalNormalizedMove(Puzzle puzzle, int moveIndex) {
        return puzzle.getMoveAt(moveIndex).trim().toLowerCase();
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
