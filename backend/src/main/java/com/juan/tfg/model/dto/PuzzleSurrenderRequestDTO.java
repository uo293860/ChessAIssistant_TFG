package com.juan.tfg.model.dto;

/**
 * Represents a request to surrender an active puzzle session.
 *
 * @param sessionId the active puzzle session identifier.
 * @param puzzleId the puzzle identifier.
 */
public record PuzzleSurrenderRequestDTO(
        Long sessionId,
        String puzzleId
) {
}
