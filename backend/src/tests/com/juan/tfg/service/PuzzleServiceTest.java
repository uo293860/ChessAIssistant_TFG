package com.juan.tfg.service;

import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.PuzzleSession;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.PuzzleDTO;
import com.juan.tfg.model.dto.PuzzleHintResponseDTO;
import com.juan.tfg.model.dto.PuzzleMoveVerificationResponseDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.PuzzleRepository;
import com.juan.tfg.repository.PuzzleSessionRepository;
import com.juan.tfg.repository.UserRepository;
import com.juan.tfg.service.aitutor.AITutorService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    private PuzzleThemeCatalog puzzleThemeCatalog;
    private PuzzleService puzzleService;

    @BeforeEach
    void setUp() {
        puzzleRepository = mock(PuzzleRepository.class);
        puzzleAttemptRepository = mock(PuzzleAttemptRepository.class);
        puzzleSessionRepository = mock(PuzzleSessionRepository.class);
        userRepository = mock(UserRepository.class);
        eloService = mock(EloService.class);
        aITutorService = mock(AITutorService.class);
        puzzleThemeCatalog = new PuzzleThemeCatalog();
        when(puzzleSessionRepository.save(any(PuzzleSession.class))).thenAnswer(invocation -> {
            PuzzleSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                session.setId(10L);
            }
            return session;
        });
        when(puzzleSessionRepository.findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan(anyString(), anyInt()))
                .thenReturn(List.of());
        when(eloService.calculateNewPlayerElo(anyInt(), anyInt(), eq(true), eq(0), eq(0))).thenReturn(1016);
        when(eloService.calculateNewPlayerElo(anyInt(), anyInt(), eq(true), eq(1), eq(0))).thenReturn(1008);
        puzzleService = new PuzzleService(
                puzzleRepository,
                puzzleAttemptRepository,
                puzzleSessionRepository,
                userRepository,
                eloService,
                aITutorService,
                puzzleThemeCatalog
        );
    }

    @Test
    @DisplayName("Returns a rated themed puzzle for an existing user")
    void getRandomPuzzleForUser() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1200)
                .build();
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1210);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("fork", 1100, 1300)).thenReturn(Optional.of(puzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "fork");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("puzzle-1");
        assertThat(result.get().sessionId()).isEqualTo(10L);
        assertThat(result.get().rating()).isEqualTo(1210);
        assertThat(result.get().hintEloPenalty()).isEqualTo(8);
        verify(eloService).calculateNewPlayerElo(1200, 1210, true, 0, 0);
        verify(eloService).calculateNewPlayerElo(1200, 1210, true, 1, 0);
    }

    @Test
    @DisplayName("Returns empty when the user does not exist")
    void getRandomPuzzleForUser_withMissingUser() {
        // Given
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("missing-user", "fork");

        // Then
        assertThat(result).isEmpty();
        verify(puzzleRepository, never()).findRandomPuzzleByThemeAndRating(any(), anyInt(), anyInt());
        verify(puzzleRepository, never()).findRandomPuzzleByRating(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Uses the default Elo rating when the user rating is null")
    void getRandomPuzzleForUser_withNullUserElo() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(null)
                .build();
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("mate", 900, 1100)).thenReturn(Optional.of(puzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "mate");

        // Then
        assertThat(result).isPresent();
        verify(puzzleRepository).findRandomPuzzleByThemeAndRating("mate", 900, 1100);
    }

    @Test
    @DisplayName("Falls back to any puzzle in the theme when the rating range has no match")
    void getRandomPuzzleForUser_withThemeNoPuzzle() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1200)
                .build();
        Puzzle fallbackPuzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1600);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("fork", 1100, 1300)).thenReturn(Optional.empty());
        when(puzzleRepository.findRandomPuzzleByTheme("fork")).thenReturn(Optional.of(fallbackPuzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "fork");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("puzzle-1");
        assertThat(result.get().rating()).isEqualTo(1600);
        verify(puzzleRepository).findRandomPuzzleByThemeAndRating("fork", 1100, 1300);
        verify(puzzleRepository).findRandomPuzzleByTheme("fork");
        verify(puzzleRepository, never()).findRandomPuzzleByRating(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Uses any puzzle in the rating range when no theme is provided")
    void getRandomPuzzleForUser_noThemeInRatingRange() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1200)
                .build();
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1210);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByRating(1100, 1300)).thenReturn(Optional.of(puzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", null);

        // Then
        assertThat(result).isPresent();
        verify(puzzleRepository).findRandomPuzzleByRating(1100, 1300);
        verify(puzzleRepository, never()).findRandomPuzzleByThemeAndRating(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Auto-surrenders an abandoned active session before returning the next puzzle")
    void getRandomPuzzleForUser_autoSurrendersOnF5() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1000)
                .build();
        Puzzle abandonedPuzzle = buildPuzzle("abandoned-puzzle", "e2e4 e7e5", 1000);
        PuzzleSession abandonedSession = buildSession(11L, user, abandonedPuzzle);
        abandonedSession.setFailedAttempts(2);
        Puzzle nextPuzzle = buildPuzzle("next-puzzle", "d2d4 d7d5", 980);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleSessionRepository.findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan("user-1", 0))
                .thenReturn(List.of(abandonedSession));
        when(eloService.calculateNewPlayerElo(1000, 1000, false, 0, 2)).thenReturn(984);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("fork", 884, 1084)).thenReturn(Optional.of(nextPuzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "fork");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("next-puzzle");
        assertThat(abandonedSession.isCompleted()).isTrue();
        assertThat(user.getEloRating()).isEqualTo(984);

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getPuzzle()).isEqualTo(abandonedPuzzle);
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isFalse();
        assertThat(attemptCaptor.getValue().getFailedAttempts()).isEqualTo(2);
        assertThat(attemptCaptor.getValue().getEloChange()).isEqualTo(-16);
        verify(puzzleRepository).findRandomPuzzleByThemeAndRating("fork", 884, 1084);
    }

    @Test
    @DisplayName("Does not auto-surrender when there are no failed active sessions")
    void getRandomPuzzleForUser_noAutoSurrenderNoFailedSessions() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .eloRating(1000)
                .build();
        Puzzle nextPuzzle = buildPuzzle("next-puzzle", "d2d4 d7d5", 1000);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleRepository.findRandomPuzzleByThemeAndRating("fork", 900, 1100)).thenReturn(Optional.of(nextPuzzle));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomPuzzleForUser("user-1", "fork");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("next-puzzle");
        assertThat(user.getEloRating()).isEqualTo(1000);
        verify(puzzleSessionRepository)
                .findByUserFirebaseUidAndCompletedFalseAndFailedAttemptsGreaterThan("user-1", 0);
        verify(puzzleAttemptRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(puzzleRepository).findRandomPuzzleByThemeAndRating("fork", 900, 1100);
    }

    @Test
    @DisplayName("Creates a retry session from a random failed puzzle attempt")
    void getRandomFailedPuzzle_withFailedAttempt() {
        // Given
        User user = buildUser("user-1", 984);
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        PuzzleAttempt failedAttempt = PuzzleAttempt.builder()
                .id(7L)
                .user(user)
                .puzzle(puzzle)
                .isSuccessful(false)
                .failedAttempts(1)
                .eloChange(-16)
                .resultingElo(984)
                .build();
        when(puzzleAttemptRepository.findRandomFailedAttempt("user-1"))
                .thenReturn(Optional.of(failedAttempt));

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomFailedPuzzleForUser("user-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("puzzle-1");
        assertThat(result.get().sessionId()).isEqualTo(10L);
        assertThat(result.get().hintEloPenalty()).isEqualTo(8);

        ArgumentCaptor<PuzzleSession> sessionCaptor = ArgumentCaptor.forClass(PuzzleSession.class);
        verify(puzzleSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(sessionCaptor.getValue().getPuzzle()).isEqualTo(puzzle);
        assertThat(sessionCaptor.getValue().getRetryAttempt()).isEqualTo(failedAttempt);
        verify(eloService).calculateNewPlayerElo(984, 1000, true, 0, 0);
        verify(eloService).calculateNewPlayerElo(984, 1000, true, 1, 0);
    }

    @Test
    @DisplayName("Returns empty when the user has no failed puzzle attempts")
    void getRandomFailedPuzzle_withNoFailedAttempt() {
        // Given
        when(puzzleAttemptRepository.findRandomFailedAttempt("user-1"))
                .thenReturn(Optional.empty());

        // When
        Optional<PuzzleDTO> result = puzzleService.getRandomFailedPuzzleForUser("user-1");

        // Then
        assertThat(result).isEmpty();
        verify(puzzleSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Generates and returns the first hint for a valid puzzle session")
    void getPuzzleHint_withValidPuzzle() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5 g1f3", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        String[] hints = {"Control the center.", "Develop a knight."};
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(aITutorService.getHints(eq("start-fen"), eq(List.of("e2e4", "e7e5", "g1f3")), eq(List.of("opening", "short"))))
                .thenReturn(hints);

        // When
        Optional<PuzzleHintResponseDTO> result = puzzleService.getPuzzleHint("user-1", 10L, "puzzle-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().hint()).isEqualTo("Control the center.");
        assertThat(result.get().hintNumber()).isEqualTo(1);
        assertThat(result.get().maxHintCount()).isEqualTo(3);
        assertThat(result.get().hintsExhausted()).isFalse();
        assertThat(session.getHintsUsed()).isEqualTo(1);
        assertThat(session.getGeneratedHints()).isEqualTo(String.join("\n", hints));
    }

    @Test
    @DisplayName("Returns stored hints one at a time without regenerating them")
    void getPuzzleHint_returnsStoredHintsOneAtATime() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5 g1f3", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        String[] hints = {"Control the center.", "Develop a knight."};
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(aITutorService.getHints(any(), any(), any())).thenReturn(hints);

        // When
        Optional<PuzzleHintResponseDTO> firstHint = puzzleService.getPuzzleHint("user-1", 10L, "puzzle-1");
        Optional<PuzzleHintResponseDTO> secondHint = puzzleService.getPuzzleHint("user-1", 10L, "puzzle-1");

        // Then
        assertThat(firstHint).isPresent();
        assertThat(firstHint.get().hint()).isEqualTo("Control the center.");
        assertThat(secondHint).isPresent();
        assertThat(secondHint.get().hint()).isEqualTo("Develop a knight.");
        assertThat(secondHint.get().hintNumber()).isEqualTo(2);
        assertThat(secondHint.get().hintsExhausted()).isTrue();
        assertThat(session.getHintsUsed()).isEqualTo(2);
        verify(aITutorService, times(1)).getHints(any(), any(), any());
    }

    @Test
    @DisplayName("Returns empty when all hints are exhausted")
    void getPuzzleHint_withExhaustedHintsReturnsEmpty() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setHintsUsed(3);
        session.setGeneratedHints("Hint 1\nHint 2\nHint 3");
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));

        // When
        Optional<PuzzleHintResponseDTO> result = puzzleService.getPuzzleHint("user-1", 10L, "puzzle-1");

        // Then
        assertThat(result).isEmpty();
        assertThat(session.getHintsUsed()).isEqualTo(3);
        verify(puzzleSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Returns empty when the puzzle id is null")
    void getPuzzleHint_withNullPuzzleId() {
        // Given
        String puzzleId = null;

        // When
        Optional<PuzzleHintResponseDTO> result = puzzleService.getPuzzleHint("user-1", 10L, puzzleId);

        // Then
        assertThat(result).isEmpty();
        verify(puzzleSessionRepository, never()).findByIdAndUserFirebaseUid(any(), any());
    }

    @Test
    @DisplayName("Propagates AI tutor failures when generating hints")
    void getPuzzleHint_withAiServiceException() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(aITutorService.getHints(any(), any(), any())).thenThrow(new IllegalStateException("AI unavailable"));

        // When
        ThrowingCallable action = () -> puzzleService.getPuzzleHint("user-1", 10L, "puzzle-1");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI unavailable");
    }

    @Test
    @DisplayName("Completes the puzzle and updates Elo after a correct final move")
    void verifyMove_withCorrectFinalMove() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, true, 0, 0)).thenReturn(1016);
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
    @DisplayName("Records penalties when a final correct move follows failures")
    void verifyMove_withCorrectFinalMoveAfterFailures() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setHintsUsed(1);
        session.setFailedAttempts(2);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, true, 1, 2)).thenReturn(987);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().newElo()).isEqualTo(987);
        assertThat(result.get().eloChange()).isEqualTo(-13);

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isFalse();
        assertThat(attemptCaptor.getValue().getFailedAttempts()).isEqualTo(2);
        assertThat(attemptCaptor.getValue().getHintsUsed()).isEqualTo(1);
    }

    @Test
    @DisplayName("Marks a retried failed puzzle as solved without changing Elo")
    void verifyMove_withRetriedFailedPuzzle() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 984);
        PuzzleAttempt failedAttempt = PuzzleAttempt.builder()
                .id(7L)
                .user(user)
                .puzzle(puzzle)
                .isSuccessful(false)
                .failedAttempts(1)
                .eloChange(-16)
                .resultingElo(984)
                .build();
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setRetryAttempt(failedAttempt);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().correct()).isTrue();
        assertThat(result.get().puzzleCompleted()).isTrue();
        assertThat(result.get().newElo()).isEqualTo(984);
        assertThat(result.get().eloChange()).isZero();
        assertThat(session.isCompleted()).isTrue();
        assertThat(failedAttempt.getIsSuccessful()).isTrue();

        verify(puzzleAttemptRepository).save(failedAttempt);
        verify(userRepository, never()).save(any());
        verify(eloService, never()).calculateNewPlayerElo(anyInt(), anyInt(), anyBoolean(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Increments failed attempts after an incorrect move")
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
    @DisplayName("Returns empty when the submitted move is blank")
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
    @DisplayName("Surrenders an active session and updates Elo as a failed attempt")
    void surrenderPuzzle_withActiveSession() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setHintsUsed(1);
        session.setFailedAttempts(1);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, false, 1, 1)).thenReturn(984);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        var result = puzzleService.surrenderPuzzle("user-1", 10L, "puzzle-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().puzzleCompleted()).isTrue();
        assertThat(result.get().newElo()).isEqualTo(984);
        assertThat(result.get().eloChange()).isEqualTo(-16);
        assertThat(session.isCompleted()).isTrue();
        assertThat(user.getEloRating()).isEqualTo(984);

        ArgumentCaptor<PuzzleAttempt> attemptCaptor = ArgumentCaptor.forClass(PuzzleAttempt.class);
        verify(puzzleAttemptRepository).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getValue().getIsSuccessful()).isFalse();
        assertThat(attemptCaptor.getValue().getHintsUsed()).isEqualTo(1);
        assertThat(attemptCaptor.getValue().getFailedAttempts()).isEqualTo(1);
        verify(eloService).calculateNewPlayerElo(1000, 1000, false, 1, 1);
        verify(userRepository).save(user);
        verify(puzzleSessionRepository).save(session);
    }

    @Test
    @DisplayName("Surrenders a retried failed puzzle without changing Elo")
    void surrenderPuzzle_withRetriedFailedPuzzle() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 984);
        PuzzleAttempt failedAttempt = PuzzleAttempt.builder()
                .id(7L)
                .user(user)
                .puzzle(puzzle)
                .isSuccessful(false)
                .failedAttempts(1)
                .eloChange(-16)
                .resultingElo(984)
                .build();
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setRetryAttempt(failedAttempt);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        // When
        var result = puzzleService.surrenderPuzzle("user-1", 10L, "puzzle-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().newElo()).isEqualTo(984);
        assertThat(result.get().eloChange()).isZero();
        assertThat(session.isCompleted()).isTrue();
        assertThat(failedAttempt.getIsSuccessful()).isFalse();

        verify(puzzleAttemptRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(eloService, never()).calculateNewPlayerElo(anyInt(), anyInt(), anyBoolean(), anyInt(), anyInt());
        verify(puzzleSessionRepository).save(session);
    }

    @Test
    @DisplayName("Returns empty when surrendering a completed session")
    void surrenderPuzzle_withCompletedSession() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setCompleted(true);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));

        // When
        var result = puzzleService.surrenderPuzzle("user-1", 10L, "puzzle-1");

        // Then
        assertThat(result).isEmpty();
        verify(puzzleAttemptRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Uses session hint and failure counters when verifying a final move")
    void verifyMove_withSessionCounters() {
        // Given
        Puzzle puzzle = buildPuzzle("puzzle-1", "e2e4 e7e5", 1000);
        User user = buildUser("user-1", 1000);
        PuzzleSession session = buildSession(10L, user, puzzle);
        session.setHintsUsed(2);
        session.setFailedAttempts(1);
        when(puzzleSessionRepository.findByIdAndUserFirebaseUid(10L, "user-1")).thenReturn(Optional.of(session));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(eloService.calculateNewPlayerElo(1000, 1000, true, 2, 1)).thenReturn(989);
        when(puzzleAttemptRepository.save(any(PuzzleAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Optional<PuzzleMoveVerificationResponseDTO> result = puzzleService.verifyMove("user-1", 10L, "puzzle-1", "e7e5");

        // Then
        assertThat(result).isPresent();
        verify(eloService).calculateNewPlayerElo(1000, 1000, true, 2, 1);
    }

    @Test
    @DisplayName("Propagates repository failures while verifying a move")
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
