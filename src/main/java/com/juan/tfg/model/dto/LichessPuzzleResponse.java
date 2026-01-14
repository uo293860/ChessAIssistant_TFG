package com.juan.tfg.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class LichessPuzzleResponse {
    private GameData game;
    private PuzzleData puzzle;

    @Data
    public static class GameData {
        private String id;
        private String pgn; // Aquí está el historial de movimientos
        // Puedes añadir 'players', 'rated', etc. si los necesitas,
        // pero para el puzzle el PGN es lo vital.
    }

    @Data
    public static class PuzzleData {
        private String id;
        private Integer rating;
        private List<String> solution;
        private Integer initialPly; // Este número es CLAVE
        private List<String> themes;
    }
}
