package com.juan.tfg.service;

import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int K_FACTOR = 32;
    private static final double PENALTY_PER_HINT = 0.25;
    private static final double PENALTY_PER_ERROR = 0.50;

    public int calculateNewPlayerElo(int playerElo, int puzzleElo, int hintsUsed, int failedAttempts) {
        double hintPenalty = hintsUsed * PENALTY_PER_HINT;
        double errorPenalty = failedAttempts * PENALTY_PER_ERROR;
        double actualScore = Math.max(0.0, 1.0 - hintPenalty - errorPenalty);

        return calculateNewPlayerEloWithScore(playerElo, puzzleElo, actualScore);
    }

    public int calculateNewPlayerEloForFailedPuzzle(int playerElo, int puzzleElo) {
        return calculateNewPlayerEloWithScore(playerElo, puzzleElo, 0.0);
    }

    private int calculateNewPlayerEloWithScore(int playerElo, int puzzleElo, double actualScore) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (puzzleElo - playerElo) / 400.0));
        double eloChange = K_FACTOR * (actualScore - expectedScore);

        return (int) Math.round(playerElo + eloChange);
    }
}
