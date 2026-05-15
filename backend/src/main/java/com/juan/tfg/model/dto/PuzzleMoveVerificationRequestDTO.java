package com.juan.tfg.model.dto;

public record PuzzleMoveVerificationRequestDTO(
        Long sessionId,
        String puzzleId,
        String move
) {
}
