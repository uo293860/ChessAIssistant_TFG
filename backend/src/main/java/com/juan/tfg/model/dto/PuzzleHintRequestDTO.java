package com.juan.tfg.model.dto;

/**
 * Represents a request for the next hint in a puzzle session.
 *
 * @param sessionId the active puzzle session identifier.
 */
public record PuzzleHintRequestDTO(
        Long sessionId
) {
}
