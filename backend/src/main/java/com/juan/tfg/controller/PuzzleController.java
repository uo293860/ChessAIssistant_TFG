package com.juan.tfg.controller;

import com.juan.tfg.model.dto.*;
import com.juan.tfg.service.PuzzleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
@RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;

    @GetMapping("/random")
    public ResponseEntity<PuzzleDTO> getRandomPuzzle(
            @AuthenticationPrincipal String firebaseUid,
            @RequestParam(defaultValue = "middlegame") String theme) {

        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return puzzleService.getRandomPuzzleForUser(firebaseUid, theme)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{puzzleId}/hints")
    public ResponseEntity<PuzzleHintResponseDTO> requestPuzzleHint(
            @AuthenticationPrincipal String firebaseUid,
            @PathVariable String puzzleId,
            @RequestBody PuzzleHintRequestDTO request
    ) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long sessionId = request == null ? null : request.sessionId();
        return puzzleService.getPuzzleHint(firebaseUid, sessionId, puzzleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/verify-move")
    public ResponseEntity<PuzzleMoveVerificationResponseDTO> verifyMove(
            @AuthenticationPrincipal String firebaseUid,
            @RequestBody PuzzleMoveVerificationRequestDTO request
    ) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return puzzleService.verifyMove(
                        firebaseUid,
                        request.sessionId(),
                        request.puzzleId(),
                        request.move()
                )
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/surrender")
    public ResponseEntity<PuzzleSurrenderResponseDTO> surrenderPuzzle(
            @AuthenticationPrincipal String firebaseUid,
            @RequestBody PuzzleSurrenderRequestDTO request
    ) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return puzzleService.surrenderPuzzle(
                        firebaseUid,
                        request.sessionId(),
                        request.puzzleId()
                )
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
