package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.repository.PuzzleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PuzzleService {

    private final PuzzleRepository puzzleRepository;
    private final AITutorService aITutorService;

    public Optional<PuzzleDTO> getRandomPuzzle(String theme, int minRating, int maxRating) {
        return puzzleRepository.findRandomPuzzleByThemeAndRating(theme, minRating, maxRating)
                .map(PuzzleDTO::from);
    }

    public Optional<String[]> getPuzzleHints(String puzzleId) {
        if (puzzleId == null || puzzleId.isBlank()) {
            return Optional.empty();
        }

        return puzzleRepository.findById(puzzleId)
                .map(this::generateHints);
    }

    public Optional<PuzzleMoveVerificationResponseDTO> verifyMove(String puzzleId, String move, int moveIndex) {
        if (puzzleId == null || puzzleId.isBlank() || move == null || move.isBlank() || moveIndex < 1) {
            return Optional.empty();
        }

        return puzzleRepository.findById(puzzleId)
                .map(puzzle -> buildVerificationResponse(puzzle, move, moveIndex));
    }

    private PuzzleMoveVerificationResponseDTO buildVerificationResponse(Puzzle puzzle, String move, int moveIndex) {
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
                    false
            );
        }

        return new PuzzleMoveVerificationResponseDTO(
                expectedMove.equals(normalizedMove),
                opponentMove,
                nextMoveIndex,
                puzzleCompleted
        );
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
