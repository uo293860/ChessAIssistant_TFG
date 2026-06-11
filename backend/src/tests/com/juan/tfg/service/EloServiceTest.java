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
        int hintsUsed = 0;
        int failedAttempts = 0;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(1016);
    }

    @Test
    void calculateNewPlayerElo_withPenalties() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;
        int hintsUsed = 1;
        int failedAttempts = 1;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(1001);
    }

    @Test
    void calculateNewPlayerElo_withManyPenalties() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;
        int hintsUsed = 10;
        int failedAttempts = 10;

        // When
        int newElo = eloService.calculateNewPlayerElo(playerElo, puzzleElo, hintsUsed, failedAttempts);

        // Then
        assertThat(newElo).isEqualTo(984);
    }

    @Test
    void calculateNewPlayerEloForFailedPuzzle() {
        // Given
        int playerElo = 1000;
        int puzzleElo = 1000;

        // When
        int newElo = eloService.calculateNewPlayerEloForFailedPuzzle(playerElo, puzzleElo);

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
        int equalPuzzleResult = eloService.calculateNewPlayerElo(playerElo, equalPuzzleElo, 0, 0);
        int harderPuzzleResult = eloService.calculateNewPlayerElo(playerElo, harderPuzzleElo, 0, 0);

        // Then
        assertThat(harderPuzzleResult).isGreaterThan(equalPuzzleResult);
    }

    @Test
    void calculateHintEloPenalty_usesTwentyFivePercentOfPossibleEloGain() {
        // When
        int equalPuzzleHintPenalty = eloService.calculateHintEloPenalty(1000, 1000);
        int harderPuzzleHintPenalty = eloService.calculateHintEloPenalty(1000, 1400);

        // Then
        assertThat(equalPuzzleHintPenalty).isEqualTo(4);
        assertThat(harderPuzzleHintPenalty).isEqualTo(7);
    }
}
