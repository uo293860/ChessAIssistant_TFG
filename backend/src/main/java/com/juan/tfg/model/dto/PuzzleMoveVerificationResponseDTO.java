package com.juan.tfg.model.dto;

/**
 * Represents the result of verifying a submitted puzzle move.
 *
 * @param correct whether the submitted move matches the expected move.
 * @param opponentMove the automatic opponent reply, if any.
 * @param nextMoveIndex the next expected move index in the solution sequence.
 * @param puzzleCompleted whether the submitted move completed the puzzle.
 * @param newElo the user's updated Elo when the puzzle is completed.
 * @param eloChange the Elo difference applied when the puzzle is completed.
 */
public record PuzzleMoveVerificationResponseDTO(
        boolean correct,
        String opponentMove,
        int nextMoveIndex,
        boolean puzzleCompleted,
        Integer newElo,
        Integer eloChange
) {
}
