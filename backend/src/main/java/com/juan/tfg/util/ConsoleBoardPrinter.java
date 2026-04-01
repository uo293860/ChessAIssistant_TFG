package com.juan.tfg.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;

public class ConsoleBoardPrinter {

    public static void printBoard(String fen) {
        Board board = new Board();
        board.loadFromFen(fen); // Carga la posición del puzzle

        System.out.println("\n  --- TABLERO ACTUAL (Juegan " + board.getSideToMove() + ") ---");

        // Bucle para pintar las filas (Rank 8 a 1)
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + " "); // Número de fila

            // Bucle para las columnas (File A a H)
            for (int file = 0; file < 8; file++) {
                Square square = Square.squareAt(rank * 8 + file);
                Piece piece = board.getPiece(square);
                System.out.print("[" + getUnicodePiece(piece) + "]");
            }
            System.out.println();
        }
        System.out.println("   A  B  C  D  E  F  G  H\n");
    }

    // Convierte las piezas internas de la librería a iconos bonitos
    private static String getUnicodePiece(Piece piece) {
        return switch (piece) {
            case WHITE_KING -> "♔";
            case WHITE_QUEEN -> "♕";
            case WHITE_ROOK -> "♖";
            case WHITE_BISHOP -> "♗";
            case WHITE_KNIGHT -> "♘";
            case WHITE_PAWN -> "♙";
            case BLACK_KING -> "♚";
            case BLACK_QUEEN -> "♛";
            case BLACK_ROOK -> "♜";
            case BLACK_BISHOP -> "♝";
            case BLACK_KNIGHT -> "♞";
            case BLACK_PAWN -> "♟";
            default -> " "; // Casilla vacía
        };
    }
}
