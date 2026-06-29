package com.juan.tfg.controller;

import com.juan.tfg.model.dto.*;
import com.juan.tfg.service.PuzzleService;
import com.juan.tfg.service.PuzzleThemeCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/puzzles")
@RequiredArgsConstructor
public class PuzzleController {

    private final PuzzleService puzzleService;
    private final PuzzleThemeCatalog puzzleThemeCatalog;

    /**
     * Returns the catalog of supported puzzle themes for authenticated users.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @return the available puzzle themes or an unauthorized response.
     */
    @GetMapping("/themes")
    public ResponseEntity<List<PuzzleThemeDTO>> getPuzzleThemes(@AuthenticationPrincipal String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(puzzleThemeCatalog.getThemes());
    }

    /**
     * Returns a random puzzle matched to the authenticated user's rating and optional theme.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @param theme the optional puzzle theme identifier.
     * @return a random puzzle, a bad request for an unknown theme, or a not-found response.
     */
    @GetMapping("/random")
    public ResponseEntity<PuzzleDTO> getRandomPuzzle(
            @AuthenticationPrincipal String firebaseUid,
            @RequestParam(name = "theme", required = false) String theme) {

        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String selectedThemeId;
        try {
            selectedThemeId = puzzleThemeCatalog.resolveSelectedThemeId(theme).orElse(null);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }

        return puzzleService.getRandomPuzzleForUser(firebaseUid, selectedThemeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reveals the next hint for an active puzzle session.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @param puzzleId the puzzle identifier from the route.
     * @param request the hint request containing the session identifier.
     * @return the next hint or a not-found response when the session is invalid.
     */
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

    /**
     * Returns a random failed puzzle attempt for the authenticated user.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @return a retry puzzle session or a not-found response when no failed attempts exist.
     */
    @PostMapping("/failed/random")
    public ResponseEntity<PuzzleDTO> getRandomFailedPuzzle(@AuthenticationPrincipal String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return puzzleService.getRandomFailedPuzzleForUser(firebaseUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Verifies a submitted puzzle move for an active session.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @param request the move verification request.
     * @return the move verification result or a not-found response when the session is invalid.
     */
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

    /**
     * Surrenders an active puzzle session and returns the solution.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @param request the surrender request.
     * @return the surrender result or a not-found response when the session is invalid.
     */
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
