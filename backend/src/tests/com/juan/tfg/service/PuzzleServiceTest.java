package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.PuzzleSession;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.PuzzleRepository;
import com.juan.tfg.repository.PuzzleSessionRepository;
import com.juan.tfg.repository.UserRepository;
import com.juan.tfg.service.aitutor.AITutorService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PuzzleServiceTest {

    private PuzzleRepository puzzleRepository;
    private PuzzleAttemptRepository puzzleAttemptRepository;
    private PuzzleSessionRepository puzzleSessionRepository;
    private UserRepository userRepository;
    private EloService eloService;
    private AITutorService aITutorService;
    private PuzzleService puzzleService;

    @BeforeEach
    void setUp() {
        puzzleRepository = mock(PuzzleRepository.class);
        puzzleAttemptRepository = mock(PuzzleAttemptRepository.class);
        puzzleSessionRepository = mock(PuzzleSessionRepository.class);
        userRepository = mock(UserRepository.class);
        eloService = mock(EloService.class);
        aITutorService = mock(AITutorService.class);
        when(puzzleSessionRepository.save(any(PuzzleSession.class))).thenAnswer(invocation -> {
            PuzzleSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(10L);
            }
            return session;
        });
        puzzleService = new PuzzleService(
                puzzleRepository,
                puzzleAttemptRepository,
                puzzleSessionRepository,
                userRepository,
                eloService,
                aITutorService
        );
    }

    @Test
    void getRandomPuzzleForUser() {
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
        assertThat(result.get().sessionId()).isEqualTo(10L);
        assertThat(result.get().rating()).isEqualTo(1210);
    }

    @Test
    void getRandomPuzzleForUser_withMissingUser() {
        // Given
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("missing-user", "fork");

        // Then
        assertThat(result).isEmpty();
        verify(puzzleRepository, never()).findRandomPuzzleByThemeAndRating(any(), anyInt(), anyInt());
    }

    @Test
    void getRandomPuzzleForUser_withNullUserElo() {
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
    void getPuzzleHints_withValidPuzzle() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5 g1f3", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        String[] hints = {"Control the center.", "Develop a knight."};
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(aITutorService.getHints(eq("start-fen"), eq(List.of("e2e4", "e7e5", "g1f3")), eq(List.of("opening", "short"))))
                .thenReturn(hints);

        // When
        Optional<String[]> result = puzzleService.getPuzzleHints("user-1", 10L, "puzzle-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly(hints);
        assertThat(session.getHintsUsed()).isEqualTo(1);
        assertThat(session.getGeneratedHints()).isEqualTo(String.join("\n", hints));
    }

    @Test
    void getPuzzleHints_withNullPuzzleId() {
        // Given
        String puzzleId = null;

        // When
        Optional<String[]> result = puzzleService.getPuzzleHints("user-1", 10L, puzzleId);

        // Then
        assertThat(result).isEmpty();
        verify(puzzleSessionRepository, never()).findByIdAndUserFirebaseUid(any(), any());
    }

    @Test
    void getPuzzleHints_withAiServiceException() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(aITutorService.getHints(any(), any(), any())).thenThrow(new IllegalStateException("AI unavailable"));

        // When
        ThrowingCallable action = () -> puzzleService.getPuzzleHints("user-1", 10L, "puzzle-1");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI unavailable");
    }

    @Test
    void verifyMove_withCorrectFinalMove() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, 0, 0)).thenReturn(1016);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().correct()).isTrue();
        assertThat(result.get().puzzleCompleted()).isTrue();
        assertThat(result.get().newElo()).isEqualTo(1016);
        assertThat(result.get().eloChange()).isEqualTo(16);
        assertThat(user.getEloRating()).isEqualTo(1016);
        assertThat(session.isCompleted()).isTrue();

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isTrue();
        assertThat(attemptCaptor.getValue().getEloChange()).isEqualTo(16);
        verify(userRepository).save(user);
    }

    @Test
    void verifyMove_withCorrectFinalMoveAfterFailures() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setHintsUsed(1);
        session.setFailedAttempts(2);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, 1, 2)).thenReturn(980);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().newElo()).isEqualTo(980);
        assertThat(result.get().eloChange()).isEqualTo(-20);

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isFalse();
        assertThat(attemptCaptor.getValue().getFailedAttempts()).isEqualTo(2);
        assertThat(attemptCaptor.getValue().getHintsUsed()).isEqualTo(1);
    }

    @Test
    void verifyMove_withIncorrectMove() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "g1f3");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().correct()).isFalse();
        assertThat(result.get().opponentMove()).isEmpty();
        assertThat(result.get().nextMoveIndex()).isEqualTo(1);
        assertThat(result.get().newElo()).isNull();
        assertThat(result.get().eloChange()).isNull();
        assertThat(session.getFailedAttempts()).isEqualTo(1);
        verify(puzzleAttemptRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyMove_withBlankMove() {
        // Given
        String blankMove = " ";

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", blankMove);

        // Then
        assertThat(result).isEmpty();
        verify(puzzleSessionRepository, never()).findByIdAndUserFirebaseUid(any(), any());
    }

    @Test
    void verifyMove_withSessionCounters() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setHintsUsed(2);
        session.setFailedAttempts(1);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, 2, 1)).thenReturn(990);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThat(result).isPresent();
        verify(eloService).calculateNewPlayerElo(1000, 1000, 2, 1);
    }

    @Test
    void verifyMove_withRepositoryException() {
        // Given
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1"))
                .thenThrow(new IllegalStateException("Database unavailable"));

        // When
        ThrowingCallable action = () -> puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database unavailable");
    }

    private User buildUser(String firebaseUid, int eloRating) {
        return User.builder()
                .firebaseUid(firebaseUid)
                .eloRating(eloRating)
                .build();
    }

    private PuzzleSession buildSession(Long id, User user, Puzzle puzzle) {
        return PuzzleSession.builder()
                .id(id)
                .user(user)
                .puzzle(puzzle)
                .nextMoveIndex(1)
                .build();
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
