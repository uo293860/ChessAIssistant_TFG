package com.juan.tfg.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EloServiceTest {

    private final EloService eloService = new EloService();

    @Test
    void calculateNewPlayerElo_withEqualRatingsAndNoPenalties() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;
        boolean solved = true;
        int hintsUsed = 0;
        int failedAttempts = 0;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, solved, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(1016);
    }

    @Test
    void calculateNewPlayerElo_withPenalties() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;
        boolean solved = true;
        int hintsUsed = 1;
        int failedAttempts = 1;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, solved, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(997);
    }

    @Test
    void calculateNewPlayerElo_withManyPenalties() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;
        boolean solved = true;
        int hintsUsed = 10;
        int failedAttempts = 10;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, solved, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(984);
    }

    @Test
    void calculateNewPlayerElo_withFailedPuzzle() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;
        boolean solved = false;
        int hintsUsed = 0;
        int failedAttempts = 0;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, solved, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(984);
    }

    @Test
    void calculateNewPlayerElo_withHarderPuzzle() {
        // Given
        int playerElo = 1000;
        int equalPuzzleElo = 1000;
        int harderPuzzleElo = 1400;

        // When
        int equalPuzzleResult = eloService.calculateNewPlayerElo(playerElo, equalPuzzleElo, true, 0, 0);
        int harderPuzzleResult = eloService.calculateNewPlayerElo(playerElo, harderPuzzleElo, true, 0, 0);

        // Then
        assertThat(harderPuzzleResult).isGreaterThan(equalPuzzleResult);
    }

    @Test
    void calculateNewPlayerElo_singleHintCostsEightEloPoints() {
        // When
        int equalPuzzleCleanSolve = eloService.calculateNewPlayerElo(1000, 1000, true, 0, 0);
        int equalPuzzleSingleHint = eloService.calculateNewPlayerElo(1000, 1000, true, 1, 0);
        int harderPuzzleCleanSolve = eloService.calculateNewPlayerElo(1000, 1400, true, 0, 0);
        int harderPuzzleSingleHint = eloService.calculateNewPlayerElo(1000, 1400, true, 1, 0);

        // Then
        assertThat(equalPuzzleCleanSolve - equalPuzzleSingleHint).isEqualTo(8);
        assertThat(harderPuzzleCleanSolve - harderPuzzleSingleHint).isEqualTo(8);
    }

    @Test
    void calculateNewPlayerElo_ignoresNegativeCounters() {
        // When
        int newElo = eloService.calculateNewPlayerElo(1000, 1000, true, -1, -1);

        // Then
        assertThat(newElo).isEqualTo(1016);
    }
}
