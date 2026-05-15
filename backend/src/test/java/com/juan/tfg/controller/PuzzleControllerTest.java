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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PuzzleControllerTest {

    private PuzzleService puzzleService;
    private PuzzleController puzzleController;

    @BeforeEach
    void setUp() {
        puzzleService = mock(PuzzleService.class);
        puzzleController = new PuzzleController(puzzleService);
    }

    @Test
    void getRandomPuzzle_withAuthenticatedUserAndPuzzle_shouldReturnOkResponse() {
        // Given
        PuzzleDTO puzzle = new PuzzleDTO("puzzle-1", "fen", 1200, "fork", "url", "e2e4");
        when(puzzleService.getRandomPuzzleForUser("user-1", "fork")).thenReturn(Optional.of(puzzle));

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle("user-1", "fork");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(puzzle);
    }

    @Test
    void getRandomPuzzle_withBlankFirebaseUid_shouldReturnUnauthorized() {
        // Given
        String firebaseUid = " ";

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle(firebaseUid, "fork");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).getRandomPuzzleForUser(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getRandomPuzzle_withMissingPuzzle_shouldReturnNotFound() {
        // Given
        when(puzzleService.getRandomPuzzleForUser("user-1", "fork")).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle("user-1", "fork");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getPuzzleHints_withExistingPuzzle_shouldReturnHints() {
        // Given
        String[] hints = {"Hint 1", "Hint 2"};
        when(puzzleService.getPuzzleHints("puzzle-1")).thenReturn(Optional.of(hints));

        // When
        ResponseEntity<String[]> response = puzzleController.getPuzzleHints("puzzle-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(hints);
    }

    @Test
    void getPuzzleHints_withMissingPuzzle_shouldReturnNotFound() {
        // Given
        when(puzzleService.getPuzzleHints("missing-puzzle")).thenReturn(Optional.empty());

        // When
        ResponseEntity<String[]> response = puzzleController.getPuzzleHints("missing-puzzle");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void verifyMove_withAuthenticatedUserAndValidRequest_shouldReturnOkResponse() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO("puzzle-1", "e7e5", 1, 0, 0);
        PuzzleMoveVerificationResponseDTO verification = new PuzzleMoveVerificationResponseDTO(true, "", 2, true, 1016);
        when(puzzleService.verifyMove("user-1", "puzzle-1", "e7e5", 1, 0, 0)).thenReturn(Optional.of(verification));

        // When
        ResponseEntity<PuzzleMoveVerificationResponseDTO> response = puzzleController.verifyMove("user-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(verification);
    }

    @Test
    void verifyMove_withNullFirebaseUid_shouldReturnUnauthorized() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO("puzzle-1", "e7e5", 1, 0, 0);

        // When
        ResponseEntity<PuzzleMoveVerificationResponseDTO> response = puzzleController.verifyMove(null, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).verifyMove(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void verifyMove_withServiceReturningEmpty_shouldReturnNotFound() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO("missing-puzzle", "e7e5", 1, 0, 0);
        when(puzzleService.verifyMove("user-1", "missing-puzzle", "e7e5", 1, 0, 0)).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleMoveVerificationResponseDTO> response = puzzleController.verifyMove("user-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void verifyMove_withServiceException_shouldPropagateException() {
        // Given
        PuzzleMoveVerificationRequestDTO request = new PuzzleMoveVerificationRequestDTO("puzzle-1", "e7e5", 1, 0, 0);
        when(puzzleService.verifyMove("user-1", "puzzle-1", "e7e5", 1, 0, 0))
                .thenThrow(new IllegalStateException("Verification failed"));

        // When
        ThrowingCallable action = () -> puzzleController.verifyMove("user-1", request);

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Verification failed");
    }
}
