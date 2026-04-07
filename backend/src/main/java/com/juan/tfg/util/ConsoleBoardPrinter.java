package com.juan.tfg.util;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

public class ConsoleBoardPrinter {

    private static final int BOARD_SIZE = 8;
    private static final int ROW_LABEL_WIDTH = 2;
    private static final int CELL_CONTENT_WIDTH = 1;

    public static void printBoard(String fen) {
        Board board = new Board();
        board.loadFromFen(fen);
        Side perspective = board.getSideToMove();

        System.out.printf("%n  --- CURRENT BOARD (%s to move) ---%n", perspective);
        System.out.println(buildColumnLabels(perspective));

        for (int displayRank = 0; displayRank < BOARD_SIZE; displayRank++) {
            StringBuilder rowBuilder = new StringBuilder();
            int rank = getRankIndex(displayRank, perspective);
            int rankLabel = getRankLabel(rank, perspective);
            rowBuilder.append(String.format("%" + ROW_LABEL_WIDTH + "d ", rankLabel));

            for (int displayFile = 0; displayFile < BOARD_SIZE; displayFile++) {
                int file = getFileIndex(displayFile, perspective);
                Square square = Square.squareAt(rank * BOARD_SIZE + file);
                Piece piece = board.getPiece(square);
                rowBuilder.append(formatCell(piece));
            }

            rowBuilder.append(' ').append(rankLabel);
            System.out.println(rowBuilder);
        }

        System.out.println(buildColumnLabels(perspective));
        System.out.println();
    }

    private static String buildColumnLabels(Side perspective) {
        StringBuilder labels = new StringBuilder("   ");
        for (int displayFile = 0; displayFile < BOARD_SIZE; displayFile++) {
            char fileLabel = getFileLabel(displayFile, perspective);
            labels.append(String.format(" %" + CELL_CONTENT_WIDTH + "s ", fileLabel));
        }
        return labels.toString();
    }

    private static int getRankIndex(int displayRank, Side perspective) {
        return perspective == Side.WHITE ? BOARD_SIZE - 1 - displayRank : displayRank;
    }

    private static int getFileIndex(int displayFile, Side perspective) {
        return perspective == Side.WHITE ? displayFile : BOARD_SIZE - 1 - displayFile;
    }

    private static int getRankLabel(int rank, Side perspective) {
        return rank + 1;
    }

    private static char getFileLabel(int displayFile, Side perspective) {
        int file = getFileIndex(displayFile, perspective);
        return (char) ('A' + file);
    }

    private static String formatCell(Piece piece) {
        return String.format("[%1$-" + CELL_CONTENT_WIDTH + "s]", getPieceSymbol(piece));
    }

    private static String getPieceSymbol(Piece piece) {
        return switch (piece) {
            case WHITE_KING -> "K";
            case WHITE_QUEEN -> "Q";
            case WHITE_ROOK -> "R";
            case WHITE_BISHOP -> "B";
            case WHITE_KNIGHT -> "N";
            case WHITE_PAWN -> "P";
            case BLACK_KING -> "k";
            case BLACK_QUEEN -> "q";
            case BLACK_ROOK -> "r";
            case BLACK_BISHOP -> "b";
            case BLACK_KNIGHT -> "n";
            case BLACK_PAWN -> "p";
            default -> " ";
        };
    }
}
