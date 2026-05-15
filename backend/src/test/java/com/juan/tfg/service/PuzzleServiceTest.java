package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.PuzzleRepository;
import com.juan.tfg.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PuzzleServiceTest {

    private PuzzleRepository puzzleRepository;
    private PuzzleAttemptRepository puzzleAttemptRepository;
    private UserRepository userRepository;
    private EloService eloService;
    private AITutorService aITutorService;
    private PuzzleService puzzleService;

    @BeforeEach
    void setUp() {
        puzzleRepository = mock(PuzzleRepository.class);
        puzzleAttemptRepository = mock(PuzzleAttemptRepository.class);
        userRepository = mock(UserRepository.class);
        eloService = mock(EloService.class);
        aITutorService = mock(AITutorService.class);
        puzzleService = new PuzzleService(
                puzzleRepository,
                puzzleAttemptRepository,
                userRepository,
                eloService,
                aITutorService
        );
    }

    @Test
    void getRandomPuzzleForUser_withExistingUserAndMatchingPuzzle_shouldReturnPuzzleDto() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1200)
                .build();
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1210);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("fork", 1150, 1250)).thenReturn(Optional.of(puzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "fork");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("puzzle-1");
        assertThat(result.get().rating()).isEqualTo(1210);
    }

    @Test
    void getRandomPuzzleForUser_withMissingUser_shouldReturnEmpty() {
        // Given
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("missing-user", "fork");

        // Then
        assertThat(result).isEmpty();
        verify(puzzleRepository, never()).findRandomPuzzleByThemeAndRating(any(), anyInt(), anyInt());
    }

    @Test
    void getRandomPuzzleForUser_withNullUserElo_shouldUseDefaultRatingRange() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(null)
                .build();
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("mate", 950, 1050)).thenReturn(Optional.of(puzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "mate");

        // Then
        assertThat(result).isPresent();
        verify(puzzleRepository).findRandomPuzzleByThemeAndRating("mate", 950, 1050);
    }

    @Test
    void getPuzzleHints_withValidPuzzle_shouldReturnGeneratedHints() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5 g1f3", 1000);
        String[] hints = {"Control the center.", "Develop a knight."};
        when(puzzleRepository.findById("puzzle-1")).thenReturn(Optional.of(puzzle));
        when(aITutorService.getHints(eq("start-fen"), eq(List.of("e2e4", "e7e5", "g1f3")), eq(List.of("opening", "short"))))
                .thenReturn(hints);

        // When
        Optional<String[]> result = puzzleService.getPuzzleHints("puzzle-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(hints);
    }

    @Test
    void getPuzzleHints_withNullPuzzleId_shouldReturnEmpty() {
        // Given
        String puzzleId = null;

        // When
        Optional<String[]> result = puzzleService.getPuzzleHints(puzzleId);

        // Then
        assertThat(result).isEmpty();
        verify(puzzleRepository, never()).findById(any());
    }

    @Test
    void getPuzzleHints_withAiServiceException_shouldPropagateException() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        when(puzzleRepository.findById("puzzle-1")).thenReturn(Optional.of(puzzle));
        when(aITutorService.getHints(any(), any(), any())).thenThrow(new IllegalStateException("AI unavailable"));

        // When
        ThrowingCallable action = () -> puzzleService.getPuzzleHints("puzzle-1");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI unavailable");
    }

    @Test
    void verifyMove_withCorrectFinalMove_shouldSaveAttemptAndUpdateUserElo() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1000)
                .build();
        when(puzzleRepository.findById("puzzle-1")).thenReturn(Optional.of(puzzle));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, 0, 0)).thenReturn(1016);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", "puzzle-1", "e7e5", 1, 0, 0);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().correct()).isTrue();
        assertThat(result.get().puzzleCompleted()).isTrue();
        assertThat(result.get().newElo()).isEqualTo(1016);
        assertThat(user.getEloRating()).isEqualTo(1016);

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isTrue();
        assertThat(attemptCaptor.getValue().getEloChange()).isEqualTo(16);
        verify(userRepository).save(user);
    }

    @Test
    void verifyMove_withCorrectFinalMoveAfterFailures_shouldSaveUnsuccessfulAttemptAndUpdateUserElo() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1000)
                .build();
        when(puzzleRepository.findById("puzzle-1")).thenReturn(Optional.of(puzzle));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, 1, 2)).thenReturn(980);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", "puzzle-1", "e7e5", 1, 1, 2);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().newElo()).isEqualTo(980);

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isFalse();
        assertThat(attemptCaptor.getValue().getFailedAttempts()).isEqualTo(2);
        assertThat(attemptCaptor.getValue().getHintsUsed()).isEqualTo(1);
    }

    @Test
    void verifyMove_withIncorrectMove_shouldReturnIncorrectAndNotSaveAttempt() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        when(puzzleRepository.findById("puzzle-1")).thenReturn(Optional.of(puzzle));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", "puzzle-1", "g1f3", 1, 0, 0);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().correct()).isFalse();
        assertThat(result.get().newElo()).isNull();
        verify(puzzleAttemptRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyMove_withBlankMove_shouldReturnEmpty() {
        // Given
        String blankMove = " ";

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", "puzzle-1", blankMove, 1, 0, 0);

        // Then
        assertThat(result).isEmpty();
        verify(puzzleRepository, never()).findById(any());
    }

    @Test
    void verifyMove_withNegativeCounters_shouldClampCountersToZero() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1000)
                .build();
        when(puzzleRepository.findById("puzzle-1")).thenReturn(Optional.of(puzzle));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, 0, 0)).thenReturn(1016);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", "puzzle-1", "e7e5", 1, -3, -2);

        // Then
        assertThat(result).isPresent();
        verify(eloService).calculateNewPlayerElo(1000, 1000, 0, 0);
    }

    @Test
    void verifyMove_withRepositoryException_shouldPropagateException() {
        // Given
        when(puzzleRepository.findById("puzzle-1")).thenThrow(new IllegalStateException("Database unavailable"));

        // When
        ThrowingCallable action = () -> puzzleService.verifyMove("user-1", "puzzle-1", "e7e5", 1, 0, 0);

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database unavailable");
    }

    private Puzzle buildPuzzle(String id, String moves, int rating) {
        return Puzzle.builder()
                .id(id)
                .fen("start-fen")
                .moves(moves)
                .rating(rating)
                .themes("opening short")
                .gameUrl("https://lichess.org/test")
                .build();
    }
}
