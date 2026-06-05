package com.juan.tfg.controller;

import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationRequestDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.service.PuzzleService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PuzzleControllerTest {

    private PuzzleService puzzleService;
    private PuzzleController puzzleController;

    @BeforeEach
    void setUp() {
        puzzleService = mock(PuzzleService.class);
        puzzleController = new PuzzleController(puzzleService);
    }

    @Test
    void getRandomPuzzle() {
        // Given
        PuzzleDTO puzzle = new PuzzleDTO("puzzle-1", 10L, "fen", 1200, "fork", "url", "e2e4");
        when(puzzleService.getRandomPuzzleForUser("user-1", "fork")).thenReturn(Optional.of(puzzle));

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle("user-1", "fork");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(puzzle);
    }

    @Test
    void getRandomPuzzle_withBlankFirebaseUid() {
        // Given
        String firebaseUid = " ";

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle(firebaseUid, "fork");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).getRandomPuzzleForUser(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getRandomPuzzle_withMissingPuzzle() {
        // Given
        when(puzzleService.getRandomPuzzleForUser("user-1", "fork")).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle("user-1", "fork");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPuzzleHints_withExistingPuzzle() {
        // Given
        String[] hints = {"Hint 1", "Hint 2"};
        when(puzzleService.getPuzzleHints("user-1", 10L, "puzzle-1")).thenReturn(Optional.of(hints));

        // When
        ResponseEntity<String[]> response = puzzleController.getPuzzleHints("user-1", "puzzle-1", 10L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(hints);
    }

    @Test
    void getPuzzleHints_withMissingPuzzle() {
        // Given
        when(puzzleService.getPuzzleHints("user-1", 10L, "missing-puzzle")).thenReturn(Optional.empty());

        // When
        ResponseEntity<String[]> response = puzzleController.getPuzzleHints("user-1", "missing-puzzle", 10L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPuzzleHints_withBlankFirebaseUid() {
        // Given
        String firebaseUid = " ";

        // When
        ResponseEntity<String[]> response = puzzleController.getPuzzleHints(firebaseUid, "puzzle-1", 10L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).getPuzzleHints(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void verifyMove() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO(10L, "puzzle-1", "e7e5");
        PuzzleMoveVerificationResponseDTO verification = new PuzzleMoveVerificationResponseDTO(true, "", 2, true, 1016, 16);
        when(puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5")).thenReturn(Optional.of(verification));

        // When
        ResponseEntity<PuzzleMoveVerificationResponseDTO> response = puzzleController.verifyMove("user-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(verification);
    }

    @Test
    void verifyMove_withNullFirebaseUid() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO(10L, "puzzle-1", "e7e5");

        // When
        ResponseEntity<PuzzleMoveVerificationResponseDTO> response = puzzleController.verifyMove(null, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).verifyMove(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void verifyMove_withServiceReturningEmpty() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO(10L, "missing-puzzle", "e7e5");
        when(puzzleService.verifyMove("user-1", 10L, "missing-puzzle", "e7e5")).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleMoveVerificationResponseDTO> response = puzzleController.verifyMove("user-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void verifyMove_withServiceException() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO(10L, "puzzle-1", "e7e5");
        when(puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5"))
                .thenThrow(new IllegalStateException("Verification failed"));

        // When
        ThrowingCallable action = () -> puzzleController.verifyMove("user-1", request);

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Verification failed");
    }
}
