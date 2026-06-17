package com.juan.tfg.service;

import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int K_FACTOR = 32;
    private static final double PENALTY_PER_HINT = 0.25;
    private static final double PENALTY_PER_ERROR = 0.33;

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
