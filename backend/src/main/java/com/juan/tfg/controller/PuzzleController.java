package com.juan.tfg.controller;

import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationRequestDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.service.PuzzleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
@CrossOrigin(origins = "http://localhost:5173") // Ajusta esto al puerto de tu React
@lombok.RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;

    @GetMapping("/random")
    public ResponseEntity<PuzzleDTO> getRandomPuzzle(
            @RequestParam(defaultValue = "middlegame") String theme,
            @RequestParam(defaultValue = "800") int minRating,
            @RequestParam(defaultValue = "1200") int maxRating) {

        return puzzleService.getRandomPuzzle(theme, minRating, maxRating)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{puzzleId}/hints")
    public ResponseEntity<String[]> getPuzzleHints(@PathVariable String puzzleId) {
        return puzzleService.getPuzzleHints(puzzleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/verify-move")
    public ResponseEntity<PuzzleMoveVerificationResponseDTO> verifyMove(
            @RequestBody PuzzleMoveVerificationRequestDTO request
    ) {
        return puzzleService.verifyMove(request.puzzleId(), request.move(), request.moveIndex())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
