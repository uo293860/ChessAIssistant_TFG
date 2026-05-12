package com.juan.tfg.model.dto;

public record PuzzleMoveVerificationResponseDTO(
        boolean correct,
        String opponentMove,
        int nextMoveIndex,
        boolean puzzleCompleted,
        Integer newElo
) {
}
