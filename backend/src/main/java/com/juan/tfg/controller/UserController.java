package com.juan.tfg.controller;

import com.juan.tfg.model.User;
import com.juan.tfg.model.dto.UserProfileDTO;
import com.juan.tfg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(@AuthenticationPrincipal String firebaseUid) {
        User user = userService.getOrCreateUser(firebaseUid);
        return ResponseEntity.ok(toProfileDTO(user));
    }

    private UserProfileDTO toProfileDTO(User user) {
        return new UserProfileDTO(
                user.getFirebaseUid(),
                user.getUsername(),
                user.getEmail(),
                user.getEloRating(),
                userService.getEloHistory(user.getFirebaseUid()),
                userService.countPuzzleAttempts(user.getFirebaseUid()),
                userService.countSolvedPuzzles(user.getFirebaseUid())
        );
    }
}
