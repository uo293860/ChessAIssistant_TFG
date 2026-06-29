package com.juan.tfg.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;

@Entity
@Table(name = "puzzles")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Puzzle {

    @Id
    @Column(length = 20)
    private String id; // Identifier from Lichess.

    @Column(nullable = false, length = 100)
    private String fen;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String moves;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String themes;

    @Column(name = "game_url")
    private String gameUrl;

    /**
     * Returns the first move from the puzzle solution sequence.
     *
     * @return the initial move, or an empty string when the puzzle has no moves.
     */
    public String getInitialMove() {
        String[] movesArray = getMovesArray();
        return movesArray.length > 0 ? movesArray[0] : "";
    }

    /**
     * Returns the move at the requested zero-based index.
     *
     * @param index the zero-based index in the puzzle move sequence.
     * @return the move at the index, or an empty string when the index is outside the sequence.
     */
    public String getMoveAt(int index) {
        String[] movesArray = getMovesArray();
        return index >= 0 && index < movesArray.length ? movesArray[index] : "";
    }

    /**
     * Counts the moves in the puzzle solution sequence.
     *
     * @return the number of parsed moves.
     */
    public int getMoveCount() {
        return getMovesArray().length;
    }

    /**
     * Parses the whitespace-separated move list into individual moves.
     *
     * @return the parsed move array without blank entries.
     */
    private String[] getMovesArray() {
        return Arrays.stream(getMoves().trim().split("\\s+"))
                .filter(move -> !move.isBlank())
                .toArray(String[]::new);
    }
}
