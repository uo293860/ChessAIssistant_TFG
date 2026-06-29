package com.juan.tfg.model.dto;

/**
 * Represents a user row in the leaderboard.
 *
 * @param username the public username.
 * @param eloRating the current Elo rating.
 * @param dailyRankChange the rank movement since the start of the day.
 * @param currentUser whether the row belongs to the authenticated user.
 */
public record UserLeaderboardEntryDTO(
        String username,
        Integer eloRating,
        Integer dailyRankChange,
        boolean currentUser
) {
}
