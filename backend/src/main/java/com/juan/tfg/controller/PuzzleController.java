package com.juan.tfg.controller;

import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.service.PuzzleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
@CrossOrigin(origins = "http://localhost:5173") // Ajusta esto al puerto de tu React
public class PuzzleController {

    @Autowired
    private PuzzleService puzzleService;

    @GetMapping("/random")
    public ResponseEntity<PuzzleDTO> getRandomPuzzle(
            @RequestParam(defaultValue = "middlegame") String theme,
            @RequestParam(defaultValue = "800") int minRating,
            @RequestParam(defaultValue = "1200") int maxRating) {

        return puzzleService.getRandomPuzzle(theme, minRating, maxRating)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
