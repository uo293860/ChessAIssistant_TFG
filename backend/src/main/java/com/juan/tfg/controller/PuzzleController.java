package com.juan.tfg.controller;

import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationRequestDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.service.PuzzleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
@CrossOrigin(origins = "http://localhost:5173")
@lombok.RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;

    @GetMapping("/random")
    public ResponseEntity<PuzzleDTO> getRandomPuzzle(
            @AuthenticationPrincipal String firebaseUid,
            @RequestParam(defaultValue = "middlegame") String theme) {

        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        return puzzleService.getRandomPuzzleForUser(firebaseUid, theme)
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
            @AuthenticationPrincipal String firebaseUid,
            @RequestBody PuzzleMoveVerificationRequestDTO request
    ) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        return puzzleService.verifyMove(
                        firebaseUid,
                        request.puzzleId(),
                        request.move(),
                        request.moveIndex(),
                        request.hintsUsed(),
                        request.failedAttempts()
                )
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
