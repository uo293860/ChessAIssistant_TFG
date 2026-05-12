package com.juan.tfg.model.dto;

public record PuzzleMoveVerificationRequestDTO(
        String puzzleId,
        String move,
        int moveIndex,
        int hintsUsed,
        int failedAttempts
) {
}
