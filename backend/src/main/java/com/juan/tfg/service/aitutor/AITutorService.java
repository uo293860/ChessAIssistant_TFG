package com.juan.tfg.service.aitutor;

import java.util.List;

public interface AITutorService {

    /**
     * Generates progressively specific hints for a chess puzzle.
     *
     * @param fen the puzzle position in FEN notation.
     * @param solution the puzzle solution moves.
     * @param themes the puzzle themes.
     * @param initialMove the initial opponent move that starts the puzzle.
     * @return generated hint lines.
     */
    String[] getHints(String fen, List<String> solution, List<String> themes, String initialMove);
}
