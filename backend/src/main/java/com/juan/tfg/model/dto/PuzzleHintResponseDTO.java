package com.juan.tfg.model.dto;

/**
 * Represents the hint revealed for a puzzle session.
 *
 * @param hint the hint text.
 * @param hintNumber the one-based number of the revealed hint.
 * @param maxHintCount the maximum number of hints exposed by the API.
 * @param hintsExhausted whether the response reveals the last available hint.
 */
public record PuzzleHintResponseDTO(
        String hint,
        int hintNumber,
        int maxHintCount,
        boolean hintsExhausted
) {
}
