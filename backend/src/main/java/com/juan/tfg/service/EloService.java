package com.juan.tfg.service;

import org.springframework.stereotype.Service;

@Service
public class EloService {

    private static final int K_FACTOR = 32;
    private static final double PENALTY_PER_HINT = 0.25;
    private static final double PENALTY_PER_ERROR = 0.33;

    public int calculateNewPlayerElo(int playerElo, int puzzleElo, int hintsUsed, int failedAttempts) {
        int cleanSuccessfulElo = calculateNewPlayerEloWithScore(playerElo, puzzleElo, 1.0);
        int failedPuzzleElo = calculateNewPlayerEloForFailedPuzzle(playerElo, puzzleElo);
        int hintPenalty = calculateHintEloPenalty(playerElo, puzzleElo) * Math.max(0, hintsUsed);
        int failedAttemptPenalty = calculateFailedAttemptEloPenalty(Math.max(0, failedAttempts));
        int penalizedElo = cleanSuccessfulElo - hintPenalty - failedAttemptPenalty;

        return Math.max(failedPuzzleElo, penalizedElo);
    }

    public int calculateNewPlayerEloForFailedPuzzle(int playerElo, int puzzleElo) {
        return calculateNewPlayerEloWithScore(playerElo, puzzleElo, 0.0);
    }

    public int calculateHintEloPenalty(int playerElo, int puzzleElo) {
        int possibleEloGain = Math.max(0, calculateNewPlayerEloWithScore(playerElo, puzzleElo, 1.0) - playerElo);
        return (int) Math.round(possibleEloGain * PENALTY_PER_HINT);
    }

    private int calculateFailedAttemptEloPenalty(int failedAttempts) {
        return (int) Math.round(K_FACTOR * PENALTY_PER_ERROR * failedAttempts);
    }

    private int calculateNewPlayerEloWithScore(int playerElo, int puzzleElo, double actualScore) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (puzzleElo - playerElo) / 400.0));
        double eloChange = K_FACTOR * (actualScore - expectedScore);

        return (int) Math.round(playerElo + eloChange);
    }
}
