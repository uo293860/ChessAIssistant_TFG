package com.juan.tfg.model.dto;

import java.time.LocalDateTime;

/**
 * Represents one Elo history point for a user profile.
 *
 * @param attemptId the identifier of the puzzle attempt that produced the rating change.
 * @param attemptDate the date and time when the attempt was created.
 * @param puzzleRating the rating of the attempted puzzle.
 * @param eloChange the Elo difference applied by the attempt.
 * @param resultingElo the user's Elo after the attempt.
 */
public record EloHistoryPointDTO(
        Long attemptId,
        LocalDateTime attemptDate,
        Integer puzzleRating,
        Integer eloChange,
        Integer resultingElo
) {
}
