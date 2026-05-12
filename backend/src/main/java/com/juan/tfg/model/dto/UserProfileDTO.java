package com.juan.tfg.model.dto;

import java.util.List;

public record UserProfileDTO(
        String firebaseUid,
        String username,
        String email,
        Integer eloRating,
        List<EloHistoryPointDTO> eloHistory
) {
}
