package com.juan.tfg.model.dto;

import com.juan.tfg.model.Puzzle;

/**
 * Represents a puzzle session returned to the frontend.
 *
 * @param id the puzzle identifier.
 * @param sessionId the active session identifier.
 * @param fen the starting position in FEN notation.
 * @param rating the puzzle rating.
 * @param themes the space-separated puzzle theme identifiers.
 * @param gameUrl the source game URL.
 * @param initialMove the first move made before the user starts solving.
 * @param hintEloPenalty the estimated Elo penalty for using one hint.
 */
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
    /**
     * Creates a puzzle DTO from a puzzle entity and session metadata.
     *
     * @param puzzle the puzzle entity to expose.
     * @param sessionId the active session identifier.
     * @param hintEloPenalty the estimated Elo penalty for using one hint.
     * @return the puzzle DTO.
     */
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
