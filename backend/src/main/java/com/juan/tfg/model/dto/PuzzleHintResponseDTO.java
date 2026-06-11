package com.juan.tfg.model.dto;

public record PuzzleHintResponseDTO(
        String hint,
        int hintNumber,
        int maxHintCount,
        boolean hintsExhausted
) {
}
