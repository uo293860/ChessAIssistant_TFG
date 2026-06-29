package com.juan.tfg.model.dto;

/**
 * Represents one selectable puzzle theme.
 *
 * @param id the Lichess puzzle theme identifier.
 * @param label the human-readable theme label.
 */
public record PuzzleThemeDTO(
        String id,
        String label
) {
}
