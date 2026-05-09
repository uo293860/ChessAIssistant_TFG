package com.juan.tfg.model.dto;

public record UserProfileDTO(
        String firebaseUid,
        String username,
        String email,
        Integer eloRating
) {
}
