package com.juan.tfg.model.dto;

/**
 * Represents a request to verify a move in an active puzzle session.
 *
 * @param sessionId the active puzzle session identifier.
 * @param puzzleId the puzzle identifier.
 * @param move the submitted move.
 */
public record PuzzleMoveVerificationRequestDTO(
        Long sessionId,
        String puzzleId,
        String move
) {
}
