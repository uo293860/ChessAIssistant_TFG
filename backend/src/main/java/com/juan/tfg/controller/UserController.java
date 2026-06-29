package com.juan.tfg.controller;

import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.UserLeaderboardEntryDTO;
import com.juan.tfg.model.dto.UserProfileDTO;
import com.juan.tfg.model.dto.UserUsernameUpdateRequestDTO;
import com.juan.tfg.service.UserService;
import com.juan.tfg.service.exception.DuplicateUsernameException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Returns users ordered by Elo rating for the leaderboard.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @return the leaderboard entries.
     */
    @GetMapping
    public ResponseEntity<List<UserLeaderboardEntryDTO>> getUsersOrderedByEloRating(@AuthenticationPrincipal String firebaseUid) {
        return ResponseEntity.ok(userService.getUsersOrderedByEloRating(firebaseUid));
    }

    /**
     * Returns the authenticated user's profile, creating it from Firebase data when needed.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @return the current user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(@AuthenticationPrincipal String firebaseUid) {
        User user = userService.getOrCreateUser(firebaseUid);
        return ResponseEntity.ok(toProfileDTO(user));
    }

    /**
     * Updates the authenticated user's username.
     *
     * @param firebaseUid the authenticated Firebase user identifier.
     * @param request the request containing the desired username.
     * @return the updated user profile.
     */
    @PatchMapping("/me/username")
    public ResponseEntity<UserProfileDTO> updateCurrentUsername(
            @AuthenticationPrincipal String firebaseUid,
            @RequestBody UserUsernameUpdateRequestDTO request
    ) {
        try {
            User user = userService.updateUsername(firebaseUid, request == null ? null : request.username());
            return ResponseEntity.ok(toProfileDTO(user));
        } catch (DuplicateUsernameException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Converts a user entity into the profile response returned by the API.
     *
     * @param user the user entity to convert.
     * @return the user profile DTO.
     */
    private UserProfileDTO toProfileDTO(User user) {
        return new UserProfileDTO(
                user.getFirebaseUid(),
                user.getUsername(),
                user.getEmail(),
                user.getEloRating(),
                userService.countPuzzleAttempts(user.getFirebaseUid()),
                userService.countSolvedPuzzles(user.getFirebaseUid()),
                userService.getEloHistory(user.getFirebaseUid())
        );
    }
}
