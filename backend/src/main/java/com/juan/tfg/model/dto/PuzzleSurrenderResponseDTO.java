package com.juan.tfg.model.dto;

public record PuzzleSurrenderResponseDTO(
        boolean puzzleCompleted,
        Integer newElo,
        Integer eloChange
) {
}
