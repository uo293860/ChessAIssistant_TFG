package com.juan.tfg.service.aitutor;

import java.util.List;

public class AbstractAITutorService implements AITutorService {

    /**
     * Returns an empty hint array for implementations that do not provide AI hints.
     *
     * @param fen the puzzle position in FEN notation.
     * @param solution the puzzle solution moves.
     * @param themes the puzzle themes.
     * @param initialMove the initial opponent move that starts the puzzle.
     * @return an empty hint array.
     */
    @Override
    public String[] getHints(String fen, List<String> solution, List<String> themes, String initialMove) {
        return new String[0];
    }

    /**
     * Builds the prompt sent to an AI tutor provider for hint generation.
     *
     * @param fen the puzzle position in FEN notation.
     * @param solution the puzzle solution moves.
     * @param themes the puzzle themes.
     * @param initialMove the initial opponent move that starts the puzzle.
     * @return the complete AI prompt.
     */
    protected String getPrompt(String fen, List<String> solution, List<String> themes, String initialMove) {
        String colour = getColourToMove(fen);
        return "You are an expert chess coach and puzzle instructor. Carefully analyze the following chess position and provide exactly three hints to help solve the problem.\n" +
                "\n" +
                "The hints must be progressively more specific:\n" +
                "\n" +
                "Hint 1 should be very general and focus on high-level evaluation or strategic ideas, identifying weaknesses and strengths over the board.\n" +
                "\n" +
                "Hint 2 should narrow the focus to relevant tactical or positional themes. Mention the specific theme of the puzzle.\n" +
                "\n" +
                "Hint 3 should make the solution quite clear, while still not explicitly stating the exact move. Mention specific squares or pieces that are especially important for the puzzle. \n" +
                "\n" +
                "Do NOT mention any specific move or explicit piece movement.\n" +
                "\n" +
                "The hints must focus on advanced chess concepts such as king safety, piece activity, weak squares, pins, skewers, discovered attacks, overloaded pieces, coordination, or tactical motifs — not on which piece to move.”\n" +
                "\n" +
                "Use precise chess terminology and the correct names of the pieces (pawn, knight, bishop, rook, queen, king).\n" +
                "\n" +
                "Write each hint as a short phrase, clearly labeled as Hint 1, Hint 2, and Hint 3.\n" +
                "The output must have strictly the following format:\n" +
                "Hint 1: Text \n" +
                "Hint 2: Text \n" +
                "Hint 3: Text \n" +
                "The tone should be instructive and clear, as if teaching a strong club player.\n" +
                "\n" +
                "The position is given in FEN format:" + fen + " where " + colour + " moves " +
                "The opponent made the move" + initialMove + "and the solution is " + solution + "\n" +
                "You can use the themes of the problem as a guide: " + themes;
    }

    /**
     * Splits provider text into individual hint lines.
     *
     * @param puzzleHints the raw provider response.
     * @return the parsed hint lines.
     */
    protected String[] parseHints(String puzzleHints) {
        //String[] answer = puzzleHints.split("\\r?\\n");
        //String[] hints = new String[answer.length-1];
        //System.arraycopy(answer, 1, hints, 0, answer.length - 1);
        return puzzleHints.split("\\r?\\n");
    }

    /**
     * Determines which color should solve the puzzle after the initial opponent move.
     *
     * @param fen the puzzle position in FEN notation.
     * @return the color name expected to move in the puzzle.
     * @throws IllegalArgumentException if the FEN is blank or does not contain a valid side-to-move field.
     */
    private String getColourToMove(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN position must not be blank.");
        }

        String[] fenFields = fen.trim().split("\\s+");
        if (fenFields.length < 2) {
            throw new IllegalArgumentException("FEN position must include the side to move.");
        }

        return switch (fenFields[1]) {
            case "w" -> "black"; // Since the fen gives the position without the oponent move we must invert the colour
            case "b" -> "white";
            default -> throw new IllegalArgumentException("FEN side to move must be either 'w' or 'b'.");
        };
    }
}
