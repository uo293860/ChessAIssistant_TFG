package com.juan.tfg.service;

import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int K_FACTOR = 32;
    private static final double PENALTY_PER_HINT = 0.25;
    private static final double PENALTY_PER_ERROR = 0.33;

    /**
     * Calculates the player's new Elo after a puzzle attempt.
     *
     * @param playerElo the player's current Elo.
     * @param puzzleElo the puzzle rating used as the opponent rating.
     * @param solved whether the puzzle was solved.
     * @param hintsUsed the number of hints used during the attempt.
     * @param failedAttempts the number of incorrect moves submitted during the attempt.
     * @return the rounded new Elo value.
     */
    public int calculateNewPlayerElo(int playerElo, int puzzleElo, boolean solved, int hintsUsed, int failedAttempts) {
        int normalizedHints = Math.max(0, hintsUsed);
        int normalizedFailedAttempts = Math.max(0, failedAttempts);
        double actualScore = solved
                ? Math.max(0.0, 1.0 - (normalizedHints * PENALTY_PER_HINT) - (normalizedFailedAttempts * PENALTY_PER_ERROR))
                : 0.0;
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (puzzleElo - playerElo) / 400.0));
        double eloChange = K_FACTOR * (actualScore - expectedScore);

        return (int) Math.round(playerElo + eloChange);
    }
}
