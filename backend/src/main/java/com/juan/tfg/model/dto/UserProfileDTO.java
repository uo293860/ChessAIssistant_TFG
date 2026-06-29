package com.juan.tfg.model.dto;

import java.util.List;

/**
 * Represents the authenticated user's profile.
 *
 * @param firebaseUid the Firebase user identifier.
 * @param username the public username.
 * @param email the user's email address.
 * @param eloRating the current Elo rating.
 * @param puzzlesAttempted the total number of attempted puzzles.
 * @param puzzlesSolved the total number of successfully solved puzzles.
 * @param eloHistory the chronological Elo history entries.
 */
public record UserProfileDTO(
        String firebaseUid,
        String username,
        String email,
        Integer eloRating,
        Long puzzlesAttempted,
        Long puzzlesSolved,
        List<EloHistoryPointDTO> eloHistory
) {
}
