package com.juan.tfg.service.lichess;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.repository.PuzzleRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
@AllArgsConstructor
public class PuzzleImporterService {

    private PuzzleRepository puzzleRepository;

    /**
     * Imports the bundled Lichess puzzle CSV when the database is empty.
     */
    @PostConstruct
    public void init() {
        long count = puzzleRepository.count();
        if (count > 0) {
            System.out.println(">>> [DATABASE] Ya existen " + count + " puzles. No se requiere importación.");
            return;
        }
        System.out.println(">>> [IMPORT] Iniciando carga de puzles desde el CSV...");
        importPuzzles();
    }

    /**
     * Reads bundled Lichess puzzles from CSV and stores a limited seed set in the database.
     */
    private void importPuzzles() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/data/lichess_db_puzzle.csv"), StandardCharsets.UTF_8))) {

            String line;
            br.readLine(); // Saltamos la cabecera
            int count = 0;

            while ((line = br.readLine()) != null && count < 10000) {
                String[] data = line.split(",");

                Puzzle puzzle = Puzzle.builder()
                        .id(data[0])
                        .fen(data[1])
                        .moves(data[2])
                        .rating(Integer.parseInt(data[3]))
                        .themes(data[7])
                        .gameUrl(data[8])
                        .build();

                puzzleRepository.save(puzzle);
                count++;

                if (count % 500 == 0) {
                    System.out.println(">>> [PROGRESS] " + count + " puzles guardados en PostgreSQL...");
                }
            }
            System.out.println(">>> [SUCCESS] Importación completada. Total: " + count + " puzles.");

        } catch (Exception e) {
            System.err.println(">>> [ERROR] Error leyendo el CSV: " + e.getMessage());
        }
    }
}
