package com.juan.tfg.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class EloServiceTest {

    private final EloService eloService = new EloService();

    @Test
    @DisplayName("Increases rating for a clean solve against an equally rated puzzle")
    void withEqualRatingsAndNoPenalties() {
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
    @DisplayName("Applies hint and failed-attempt penalties to a solved puzzle")
    void withPenalties() {
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
    @DisplayName("Caps penalties when many hints and failed attempts are used")
    void withManyPenalties() {
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
    @DisplayName("Decreases rating when the puzzle is failed")
    void withFailedPuzzle() {
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
    @DisplayName("Awards more Elo for solving a harder puzzle")
    void withHarderPuzzle() {
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
    @DisplayName("Subtracts eight Elo points for a single hint")
    void singleHintCostsEightEloPoints() {
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
    @DisplayName("Ignores negative hint and failed-attempt counters")
    void ignoresNegativeCounters() {
        // When
        int newElo = eloService.calculateNewPlayerElo(1000, 1000, true, -1, -1);

        // Then
        assertThat(newElo).isEqualTo(1016);
    }
}
