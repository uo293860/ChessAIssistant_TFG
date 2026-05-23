package com.juan.tfg.model;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PuzzleTest {

    @Test
    void getInitialMove_withMultipleMoves() {
        // Given
        Puzzle puzzle = Puzzle.builder()
                .moves("e2e4 e7e5 g1f3")
                .build();

        // When
        String initialMove = puzzle.getInitialMove();

        // Then
        assertThat(initialMove).isEqualTo("e2e4");
    }

    @Test
    void getMoveAtIndex() {
        // Given
        Puzzle puzzle = Puzzle.builder()
                .moves("e2e4 e7e5 g1f3")
                .build();

        // When
        String move = puzzle.getMoveAt(2);

        // Then
        assertThat(move).isEqualTo("g1f3");
    }

    @Test
    void getMoveAtIndexOR() {
        // Given
        Puzzle puzzle = Puzzle.builder()
                .moves("e2e4 e7e5")
                .build();

        // When
        String move = puzzle.getMoveAt(10);

        // Then
        assertThat(move).isEmpty();
    }

    @Test
    void getMoveCount_withBlankMoves() {
        // Given
        Puzzle puzzle = Puzzle.builder()
                .moves("   ")
                .build();

        // When
        int moveCount = puzzle.getMoveCount();

        // Then
        assertThat(moveCount).isZero();
    }

    @Test
    void getInitialMove_withNullMoves() {
        // Given
        Puzzle puzzle = Puzzle.builder()
                .moves(null)
                .build();

        // When
        ThrowingCallable action = puzzle::getInitialMove;

        // Then
        assertThatThrownBy(action).isInstanceOf(NullPointerException.class);
    }
}
