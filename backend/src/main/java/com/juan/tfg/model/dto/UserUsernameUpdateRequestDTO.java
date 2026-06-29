package com.juan.tfg.model.dto;

/**
 * Represents a request to update the authenticated user's username.
 *
 * @param username the requested username.
 */
public record UserUsernameUpdateRequestDTO(String username) {
}
