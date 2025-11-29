package com.juan.tfg.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.game.Game;
import com.github.bhlangonijr.chesslib.pgn.PgnHolder;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.util.ArrayList;

public class PgnToBoard {

    /**
     * Convierte un PGN y un número de jugada (ply) en un objeto Board listo para pintar/analizar.
     */
    public static Board getBoardFromPgn(String pgnString, int initialPly) {
        try {
            // 1. Cargamos el PGN en un contenedor de chesslib
            PgnHolder pgn = new PgnHolder(null);
            pgn.loadPgn(new java.io.StringReader(pgnString));

            // 2. Extraemos la partida (Lichess devuelve solo 1 partida por JSON)
            if (pgn.getGames().isEmpty()) return new Board(); // Retorna tablero vacío si falla
            Game game = pgn.getGames().get(0);

            // 3. Cargamos los movimientos de la partida
            game.loadMoveList();
            MoveList moves = game.getHalfMoves();

            // 4. Creamos un tablero y reproducimos los movimientos hasta 'initialPly'
            Board board = new Board();
            // initialPly es el número de medios-movimientos (blancas + negras)
            for (int i = 0; i < initialPly && i < moves.size(); i++) {
                board.doMove(moves.get(i));
            }

            return board;

        } catch (Exception e) {
            System.err.println("Error parseando PGN: " + e.getMessage());
            return new Board();
        }
    }
}
