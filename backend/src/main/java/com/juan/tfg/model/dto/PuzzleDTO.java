package com.juan.tfg.model.dto;

import com.juan.tfg.model.Puzzle;

public record PuzzleDTO(
        String id,
        Long sessionId,
        String fen,
        Integer rating,
        String themes,
        String gameUrl,
        String initialMove,
        int hintEloPenalty
) {
    public static PuzzleDTO from(Puzzle puzzle, Long sessionId, int hintEloPenalty) {
        return new PuzzleDTO(
                puzzle.getId(),
                sessionId,
                puzzle.getFen(),
                puzzle.getRating(),
                puzzle.getThemes(),
                puzzle.getGameUrl(),
                puzzle.getInitialMove(),
                hintEloPenalty
        );
    }
}
