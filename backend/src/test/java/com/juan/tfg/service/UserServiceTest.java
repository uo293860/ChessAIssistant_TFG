package com.juan.tfg.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseToken;
import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.EloHistoryPointDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private FirebaseApp firebaseApp;
    private PuzzleAttemptRepository puzzleAttemptRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        firebaseApp = mock(FirebaseApp.class);
        puzzleAttemptRepository = mock(PuzzleAttemptRepository.class);
        userService = new UserService(userRepository, firebaseApp, puzzleAttemptRepository);
    }

    @Test
    void getOrCreateUser_withExistingFirebaseTokenUser() {
        // Given
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        User existingUser = User.builder()
                .firebaseUid("firebase-uid")
                .email("player@example.com")
                .username("player")
                .build();
        when(firebaseToken.getUid()).thenReturn("firebase-uid");
        when(userRepository.findById("firebase-uid")).thenReturn(Optional.of(existingUser));

        // When
        User result = userService.getOrCreateUser(firebaseToken);

        // Then
        assertThat(result).isSameAs(existingUser);
    }

    @Test
    void getOrCreateUser_withNewFirebaseTokenUser() {
        // Given
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("ABC123456789");
        when(firebaseToken.getEmail()).thenReturn("PLAYER@Example.COM");
        when(firebaseToken.getName()).thenReturn("Player One!");
        when(userRepository.findById("ABC123456789")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("player-one")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.getOrCreateUser(firebaseToken);

        // Then
        assertThat(result.getFirebaseUid()).isEqualTo("ABC123456789");
        assertThat(result.getEmail()).isEqualTo("player@example.com");
        assertThat(result.getUsername()).isEqualTo("player-one");
    }

    @Test
    void getOrCreateUser_withMissingFirebaseEmailAndName() {
        // Given
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("UID987654321");
        when(firebaseToken.getEmail()).thenReturn(null);
        when(firebaseToken.getName()).thenReturn(null);
        when(userRepository.findById("UID987654321")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("uid987654321")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.getOrCreateUser(firebaseToken);

        // Then
        assertThat(result.getEmail()).isEqualTo("UID987654321@firebase.local");
        assertThat(result.getUsername()).isEqualTo("uid987654321");
    }

    @Test
    void getOrCreateUser_withDuplicateUsername() {
        // Given
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("ABC123456789");
        when(firebaseToken.getEmail()).thenReturn("player@example.com");
        when(firebaseToken.getName()).thenReturn("Player");
        when(userRepository.findById("ABC123456789")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("player")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.getOrCreateUser(firebaseToken);

        // Then
        assertThat(result.getUsername()).isEqualTo("player-abc12345");
    }

    @Test
    void getEloHistory_withAttempts() {
        // Given
        LocalDateTime attemptDate = LocalDateTime.of(2026, 5, 15, 10, 30);
        Puzzle puzzle = Puzzle.builder()
                .id("puzzle-1")
                .rating(1200)
                .build();
        PuzzleAttempt attempt = PuzzleAttempt.builder()
                .id(1L)
                .puzzle(puzzle)
                .attemptDate(attemptDate)
                .eloChange(12)
                .resultingElo(1012)
                .build();
        when(puzzleAttemptRepository.findEloHistoryByUserId("user-1")).thenReturn(List.of(attempt));

        // When
        List<EloHistoryPointDTO> result = userService.getEloHistory("user-1");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().attemptId()).isEqualTo(1L);
        assertThat(result.getFirst().attemptDate()).isEqualTo(attemptDate);
        assertThat(result.getFirst().puzzleRating()).isEqualTo(1200);
        assertThat(result.getFirst().eloChange()).isEqualTo(12);
        assertThat(result.getFirst().resultingElo()).isEqualTo(1012);
    }

    @Test
    void getEloHistory_withEmptyRepositoryResult() {
        // Given
        when(puzzleAttemptRepository.findEloHistoryByUserId("user-1")).thenReturn(List.of());

        // When
        List<EloHistoryPointDTO> result = userService.getEloHistory("user-1");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getEloHistory_withRepositoryException() {
        // Given
        when(puzzleAttemptRepository.findEloHistoryByUserId("user-1")).thenThrow(new IllegalStateException("Database unavailable"));

        // When
        ThrowingCallable action = () -> userService.getEloHistory("user-1");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database unavailable");
    }

    @Test
    void countPuzzleAttempts_withExistingAttempts() {
        // Given
        when(puzzleAttemptRepository.countByFirebaseUid("user-1")).thenReturn(5L);

        // When
        long result = userService.countPuzzleAttempts("user-1");

        // Then
        assertThat(result).isEqualTo(5L);
        verify(puzzleAttemptRepository).countByFirebaseUid("user-1");
    }

    @Test
    void countSolvedPuzzles_withExistingSolvedAttempts() {
        // Given
        when(puzzleAttemptRepository.countSuccessfulByFirebaseUid("user-1")).thenReturn(3L);

        // When
        long result = userService.countSolvedPuzzles("user-1");

        // Then
        assertThat(result).isEqualTo(3L);
        verify(puzzleAttemptRepository).countSuccessfulByFirebaseUid("user-1");
    }
}
