package com.juan.tfg.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseToken;
import com.juan.tfg.model.Puzzle;
import com.juan.tfg.model.PuzzleAttempt;
import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.EloHistoryPointDTO;
import com.juan.tfg.model.dto.UserLeaderboardEntryDTO;
import com.juan.tfg.repository.PuzzleAttemptRepository;
import com.juan.tfg.repository.UserRepository;
import com.juan.tfg.service.exception.DuplicateUsernameException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.getOrCreateUser(firebaseToken);

        // Then
        assertThat(result.getEmail()).isEqualTo("UID987654321@firebase.local");
        assertThat(result.getUsername()).isEqualTo("user");
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
        assertThat(result.getUsername())
                .startsWith("player-")
                .matches("player-\\d{6}")
                .doesNotContain("abc12345");
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
    void updateUsername_withValidUsername() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("old-player")
                .email("player@example.com")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndFirebaseUidNot("new-player", "user-1")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        // When
        User result = userService.updateUsername("user-1", "New Player!");

        // Then
        assertThat(result.getUsername()).isEqualTo("new-player");
        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_withSameUsername() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("player")
                .email("player@example.com")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        // When
        User result = userService.updateUsername("user-1", "player");

        // Then
        assertThat(result).isSameAs(user);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUsername_withInvalidUsername() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("player")
                .email("player@example.com")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        // When
        ThrowingCallable action = () -> userService.updateUsername("user-1", "!!!");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must include at least one letter or number.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUsername_withDuplicateUsername() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("player")
                .email("player@example.com")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndFirebaseUidNot("taken", "user-1")).thenReturn(true);

        // When
        ThrowingCallable action = () -> userService.updateUsername("user-1", "taken");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessage("Username is already in use.");
        verify(userRepository, never()).save(any(User.class));
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
    void getUsersOrderedByEloRating() {
        // Given
        User highestRatedUser = User.builder()
                .firebaseUid("user-2")
                .username("highest")
                .email("highest@example.com")
                .eloRating(1600)
                .build();
        User lowerRatedUser = User.builder()
                .firebaseUid("user-1")
                .username("lower")
                .email("lower@example.com")
                .eloRating(1200)
                .build();
        when(userRepository.findAllByOrderByEloRatingDescUsernameAsc())
                .thenReturn(List.of(highestRatedUser, lowerRatedUser));
        when(puzzleAttemptRepository.findDailyEloChangesSince(any(LocalDateTime.class))).thenReturn(List.of());

        // When
        List<UserLeaderboardEntryDTO> result = userService.getUsersOrderedByEloRating("user-1");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::username)
                .containsExactly("highest", "lower");
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::eloRating)
                .containsExactly(1600, 1200);
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::dailyRankChange)
                .containsExactly(0, 0);
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::currentUser)
                .containsExactly(false, true);
        verify(userRepository).findAllByOrderByEloRatingDescUsernameAsc();
    }

    @Test
    void getUsersOrderedByEloRating_withDailyRankChanges() {
        // Given
        User climber = User.builder()
                .firebaseUid("user-1")
                .username("climber")
                .email("climber@example.com")
                .eloRating(1300)
                .build();
        User steady = User.builder()
                .firebaseUid("user-2")
                .username("steady")
                .email("steady@example.com")
                .eloRating(1250)
                .build();
        User descending = User.builder()
                .firebaseUid("user-3")
                .username("descending")
                .email("descending@example.com")
                .eloRating(1200)
                .build();
        when(userRepository.findAllByOrderByEloRatingDescUsernameAsc())
                .thenReturn(List.of(climber, steady, descending));
        when(puzzleAttemptRepository.findDailyEloChangesSince(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        dailyEloChange("user-1", 350L),
                        dailyEloChange("user-3", -100L)
                ));

        // When
        List<UserLeaderboardEntryDTO> result = userService.getUsersOrderedByEloRating("user-1");

        // Then
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::username)
                .containsExactly("climber", "steady", "descending");
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::dailyRankChange)
                .containsExactly(2, 0, -2);
        assertThat(result)
                .extracting(UserLeaderboardEntryDTO::currentUser)
                .containsExactly(true, false, false);
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

    private PuzzleAttemptRepository.UserDailyEloChange dailyEloChange(String firebaseUid, Long eloChange) {
        return new PuzzleAttemptRepository.UserDailyEloChange() {
            @Override
            public String getFirebaseUid() {
                return firebaseUid;
            }

            @Override
            public Long getEloChange() {
                return eloChange;
            }
        };
    }
}
