package com.juan.tfg.service.aitutor;

import java.util.List;

public class AbstractAITutorService implements AITutorService {
    @Override
    public String[] getHints(String fen, List<String> solution, List<String> themes) {
        return new String[0];
    }

    protected String getPrompt(String fen, List<String> solution, List<String> themes) {
        return "You are an expert chess coach and puzzle instructor. Carefully analyze the following chess position and provide exactly three hints to help solve the problem.\n" +
                "\n" +
                "The hints must be progressively more specific:\n" +
                "\n" +
                "Hint 1 should be very general and focus on high-level evaluation or strategic ideas, identifying weaknesses and strengths over the board.\n" +
                "\n" +
                "Hint 2 should narrow the focus to relevant tactical or positional themes.\n" +
                "\n" +
                "Hint 3 should make the solution quite clear, while still not explicitly stating the exact move.\n" +
                "\n" +
                "Do NOT mention any specific move, square, or explicit piece movement.\n" +
                "\n" +
                "The hints must focus on advanced chess concepts such as king safety, piece activity, weak squares, pins, skewers, discovered attacks, overloaded pieces, coordination, or tactical motifs — not on “which piece to move.”\n" +
                "\n" +
                "Use precise chess terminology and the correct names of the pieces (pawn, knight, bishop, rook, queen, king).\n" +
                "\n" +
                "Write each hint as a short phrase, clearly labeled as Hint 1, Hint 2, and Hint 3.\n" +
                "Use the following format:\n" +
                "Hint 1: Text \n" +
                "Hint 2: Text \n" +
                "Hint 3: Text \n" +
                "The tone should be instructive and clear, as if teaching a strong club player.\n" +
                "\n" +
                "The position is given in FEN format:" + fen + " and the solution is " + solution + "\n" +
                "You can use the themes of the problem as a guide: " + themes;
    }

    protected String[] parseHints(String puzzleHints) {
        return puzzleHints.split("\\r?\\n");
    }
}
