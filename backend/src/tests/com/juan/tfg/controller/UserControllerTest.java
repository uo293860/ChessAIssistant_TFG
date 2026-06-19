package com.juan.tfg.controller;

import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.EloHistoryPointDTO;
import com.juan.tfg.model.dto.UserLeaderboardEntryDTO;
import com.juan.tfg.model.dto.UserProfileDTO;
import com.juan.tfg.model.dto.UserUsernameUpdateRequestDTO;
import com.juan.tfg.service.UserService;
import com.juan.tfg.service.exception.DuplicateUsernameException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
    }

    @Test
    @DisplayName("Returns the current user profile with puzzle statistics and Elo history")
    void getCurrentUser_withExistingUser() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("player")
                .email("player@example.com")
                .eloRating(1016)
                .build();
        List<EloHistoryPointDTO> eloHistory = List.of(new EloHistoryPointDTO(
                1L,
                LocalDateTime.of(2026, 5, 15, 12, 0),
                1200,
                16,
                1016
        ));
        when(userService.getOrCreateUser("user-1")).thenReturn(user);
        when(userService.countPuzzleAttempts("user-1")).thenReturn(4L);
        when(userService.countSolvedPuzzles("user-1")).thenReturn(3L);
        when(userService.getEloHistory("user-1")).thenReturn(eloHistory);

        // When
        ResponseEntity<UserProfileDTO> response = userController.getCurrentUser("user-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().firebaseUid()).isEqualTo("user-1");
        assertThat(response.getBody().username()).isEqualTo("player");
        assertThat(response.getBody().email()).isEqualTo("player@example.com");
        assertThat(response.getBody().eloRating()).isEqualTo(1016);
        assertThat(response.getBody().puzzlesAttempted()).isEqualTo(4L);
        assertThat(response.getBody().puzzlesSolved()).isEqualTo(3L);
        assertThat(response.getBody().eloHistory()).isEqualTo(eloHistory);
    }

    @Test
    @DisplayName("Returns users ordered by Elo rating for the leaderboard")
    void getUsersOrderedByEloRating() {
        // Given
        List<UserLeaderboardEntryDTO> leaderboard = List.of(
                new UserLeaderboardEntryDTO("highest", 1600, 0, false),
                new UserLeaderboardEntryDTO("lower", 1200, 0, true)
        );
        when(userService.getUsersOrderedByEloRating("user-1")).thenReturn(leaderboard);

        // When
        ResponseEntity<List<UserLeaderboardEntryDTO>> response = userController.getUsersOrderedByEloRating("user-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(leaderboard);
    }

    @Test
    @DisplayName("Updates the current user's username and returns the refreshed profile")
    void updateCurrentUsername_withValidUsername() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("new-player")
                .email("player@example.com")
                .eloRating(1016)
                .build();
        when(userService.updateUsername("user-1", "New Player"))
                .thenReturn(user);
        when(userService.countPuzzleAttempts("user-1")).thenReturn(4L);
        when(userService.countSolvedPuzzles("user-1")).thenReturn(3L);
        when(userService.getEloHistory("user-1")).thenReturn(List.of());

        // When
        ResponseEntity<UserProfileDTO> response = userController.updateCurrentUsername(
                "user-1",
                new UserUsernameUpdateRequestDTO("New Player")
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().username()).isEqualTo("new-player");
        verify(userService).updateUsername("user-1", "New Player");
    }

    @Test
    @DisplayName("Returns bad request when the username update is invalid")
    void updateCurrentUsername_withInvalidUsername() {
        // Given
        when(userService.updateUsername("user-1", "!!!"))
                .thenThrow(new IllegalArgumentException("Username must include at least one letter or number."));

        // When
        ThrowingCallable action = () -> userController.updateCurrentUsername(
                "user-1",
                new UserUsernameUpdateRequestDTO("!!!")
        );

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Returns conflict when the username update duplicates another user")
    void updateCurrentUsername_withDuplicateUsername() {
        // Given
        when(userService.updateUsername("user-1", "taken"))
                .thenThrow(new DuplicateUsernameException("Username is already in use."));

        // When
        ThrowingCallable action = () -> userController.updateCurrentUsername(
                "user-1",
                new UserUsernameUpdateRequestDTO("taken")
        );

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Returns the current user profile with an empty Elo history")
    void getCurrentUser_withEmptyEloHistory() {
        // Given
        User user = User.builder()
                .firebaseUid("user-1")
                .username("player")
                .email("player@example.com")
                .eloRating(1000)
                .build();
        when(userService.getOrCreateUser("user-1")).thenReturn(user);
        when(userService.countPuzzleAttempts("user-1")).thenReturn(0L);
        when(userService.countSolvedPuzzles("user-1")).thenReturn(0L);
        when(userService.getEloHistory("user-1")).thenReturn(List.of());

        // When
        ResponseEntity<UserProfileDTO> response = userController.getCurrentUser("user-1");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().eloHistory()).isEmpty();
    }

    @Test
    @DisplayName("Propagates service failures while loading the current user profile")
    void getCurrentUser_withServiceException() {
        // Given
        when(userService.getOrCreateUser("user-1")).thenThrow(new IllegalStateException("Firebase unavailable"));

        // When
        ThrowingCallable action = () -> userController.getCurrentUser("user-1");

        // Then
        assertThatThrownBy(action)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Firebase unavailable");
    }
}
