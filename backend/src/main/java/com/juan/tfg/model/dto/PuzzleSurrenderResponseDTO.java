package com.juan.tfg.model.dto;

import java.util.List;

public record PuzzleSurrenderResponseDTO(
        boolean puzzleCompleted,
        Integer newElo,
        Integer eloChange,
        List<String> solutionMoves
) {
}
