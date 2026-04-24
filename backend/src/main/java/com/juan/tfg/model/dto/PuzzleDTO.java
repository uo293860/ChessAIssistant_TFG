package com.juan.tfg.model.dto;

import com.juan.tfg.model.Puzzle;

public record PuzzleDTO(
        String id,
        String fen,
        Integer rating,
        String themes,
        String gameUrl,
        String initialMove
) {

    public static PuzzleDTO from(Puzzle puzzle) {
        return new PuzzleDTO(
                puzzle.getId(),
                puzzle.getFen(),
                puzzle.getRating(),
                puzzle.getThemes(),
                puzzle.getGameUrl(),
                puzzle.getInitialMove()
        );
    }
}
