package com.juan.tfg.model;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PuzzleTest {

    @Test
    @DisplayName("Returns the first move when multiple moves are present")
    void getInitialMoveMultipleMoves() {
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
    @DisplayName("Returns the move at the requested index")
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
    @DisplayName("Returns an empty string when the requested move index is out of bounds")
    void getMoveAtIndexOB() {
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
    @DisplayName("Returns zero when the moves string is blank")
    void getMoveCountBlankMoves() {
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
    @DisplayName("Throws a NullPointerException when moves are null")
    void getInitialMoveNullMoves() {
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
