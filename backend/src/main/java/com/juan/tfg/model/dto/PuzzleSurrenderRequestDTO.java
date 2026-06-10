package com.juan.tfg.model.dto;

public record PuzzleSurrenderRequestDTO(
        Long sessionId,
        String puzzleId
) {
}
