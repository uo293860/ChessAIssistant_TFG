package com.juan.tfg.controller;

import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleHintRequestDTO;
import com.juan.tfg.model.dto.PuzzleHintResponseDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationRequestDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.model.dto.PuzzleSurrenderRequestDTO;
import com.juan.tfg.model.dto.PuzzleSurrenderResponseDTO;
import com.juan.tfg.service.PuzzleService;
import com.juan.tfg.service.PuzzleThemeCatalog;
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
    private PuzzleThemeCatalog puzzleThemeCatalog;
    private PuzzleController puzzleController;

    @BeforeEach
    void setUp() {
        puzzleService = mock(PuzzleService.class);
        puzzleThemeCatalog = new PuzzleThemeCatalog();
        puzzleController = new PuzzleController(puzzleService, puzzleThemeCatalog);
    }

    @Test
    void getRandomPuzzle() {
        // Given
        PuzzleDTO puzzle = new PuzzleDTO("puzzle-1", 10L, "fen", 1200, "fork", "url", "e2e4", 4);
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
    void getRandomPuzzle_withoutTheme() {
        // Given
        PuzzleDTO puzzle = new PuzzleDTO("puzzle-1", 10L, "fen", 1200, "fork", "url", "e2e4", 4);
        when(puzzleService.getRandomPuzzleForUser("user-1", null)).thenReturn(Optional.of(puzzle));

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle("user-1", null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(puzzle);
    }

    @Test
    void getRandomPuzzle_withUnknownTheme() {
        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomPuzzle("user-1", "unknownTheme");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(puzzleService, never()).getRandomPuzzleForUser(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getPuzzleThemes() {
        // When
        var response = puzzleController.getPuzzleThemes("user-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).extracting("id").contains("fork", "middlegame", "enPassant");
    }

    @Test
    void getPuzzleThemes_withBlankFirebaseUid() {
        // When
        var response = puzzleController.getPuzzleThemes(" ");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requestPuzzleHint_withExistingPuzzle() {
        // Given
        PuzzleHintRequestDTO request = new PuzzleHintRequestDTO(10L);
        PuzzleHintResponseDTO hint = new PuzzleHintResponseDTO("Hint 1", 1, 3, false);
        when(puzzleService.getPuzzleHint("user-1", 10L, "puzzle-1")).thenReturn(Optional.of(hint));

        // When
        ResponseEntity<PuzzleHintResponseDTO> response = puzzleController.requestPuzzleHint("user-1", "puzzle-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(hint);
    }

    @Test
    void requestPuzzleHint_withMissingPuzzle() {
        // Given
        PuzzleHintRequestDTO request = new PuzzleHintRequestDTO(10L);
        when(puzzleService.getPuzzleHint("user-1", 10L, "missing-puzzle")).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleHintResponseDTO> response = puzzleController.requestPuzzleHint("user-1", "missing-puzzle", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void requestPuzzleHint_withBlankFirebaseUid() {
        // Given
        String firebaseUid = " ";
        PuzzleHintRequestDTO request = new PuzzleHintRequestDTO(10L);

        // When
        ResponseEntity<PuzzleHintResponseDTO> response = puzzleController.requestPuzzleHint(firebaseUid, "puzzle-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).getPuzzleHint(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getRandomFailedPuzzle() {
        // Given
        PuzzleDTO puzzle = new PuzzleDTO("puzzle-1", 11L, "fen", 1200, "fork", "url", "e2e4", 4);
        when(puzzleService.getRandomFailedPuzzleForUser("user-1")).thenReturn(Optional.of(puzzle));

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomFailedPuzzle("user-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(puzzle);
    }

    @Test
    void getRandomFailedPuzzle_withBlankFirebaseUid() {
        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomFailedPuzzle(" ");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).getRandomFailedPuzzleForUser(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void getRandomFailedPuzzle_withNoFailedAttempt() {
        // Given
        when(puzzleService.getRandomFailedPuzzleForUser("user-1")).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleDTO> response = puzzleController.getRandomFailedPuzzle("user-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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

    @Test
    void surrenderPuzzle() {
        // Given
        PuzzleSurrenderRequestDTO request = new PuzzleSurrenderRequestDTO(10L, "puzzle-1");
        PuzzleSurrenderResponseDTO surrender = new PuzzleSurrenderResponseDTO(true, 984, -16);
        when(puzzleService.surrenderPuzzle("user-1", 10L, "puzzle-1")).thenReturn(Optional.of(surrender));

        // When
        ResponseEntity<PuzzleSurrenderResponseDTO> response = puzzleController.surrenderPuzzle("user-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(surrender);
    }

    @Test
    void surrenderPuzzle_withBlankFirebaseUid() {
        // Given
        PuzzleSurrenderRequestDTO request = new PuzzleSurrenderRequestDTO(10L, "puzzle-1");

        // When
        ResponseEntity<PuzzleSurrenderResponseDTO> response = puzzleController.surrenderPuzzle(" ", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(puzzleService, never()).surrenderPuzzle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void surrenderPuzzle_withServiceReturningEmpty() {
        // Given
        PuzzleSurrenderRequestDTO request = new PuzzleSurrenderRequestDTO(10L, "missing-puzzle");
        when(puzzleService.surrenderPuzzle("user-1", 10L, "missing-puzzle")).thenReturn(Optional.empty());

        // When
        ResponseEntity<PuzzleSurrenderResponseDTO> response = puzzleController.surrenderPuzzle("user-1", request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
