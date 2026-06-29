package com.juan.tfg.model.dto;

import java.util.List;

/**
 * Represents the result of surrendering a puzzle session.
 *
 * @param puzzleCompleted whether the puzzle session was completed by surrendering.
 * @param newElo the user's updated Elo.
 * @param eloChange the Elo difference applied by the surrender.
 * @param solutionMoves the remaining solution moves after the initial move.
 */
public record PuzzleSurrenderResponseDTO(
        boolean puzzleCompleted,
        Integer newElo,
        Integer eloChange,
        List<String> solutionMoves
) {
}
