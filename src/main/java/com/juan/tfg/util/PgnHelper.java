package com.juan.tfg.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.move.MoveList;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;

import java.io.StringReader;

public class PgnHelper {

    /**
     * Convierte un PGN completo a un FEN en un momento específico (ply).
     * Ideal para los puzzles de Lichess.
     *
     * @param pgnRaw El string con toda la partida (ej: "1. e4 e5 2. Nf3...")
     * @param ply    El número de medio-movimiento donde quieres la foto (ej: 60)
     * @return El string FEN resultante (ej: "rnbqk...")
     */
    public static String getFenFromPgn(String pgnRaw, int ply) {
        if (pgnRaw == null || pgnRaw.isEmpty()) {
            return null;
        }

        try {
            MoveList list = new MoveList();
            list.loadFromSan(pgnRaw);
            String fen = list.getFen();
            System.out.println("FEN of final position: " + fen);


            return fen;

        } catch (Exception e) {
            System.err.println("❌ Error crítico parseando PGN: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
