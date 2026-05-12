package com.juan.tfg.model.dto;

import java.util.List;

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
