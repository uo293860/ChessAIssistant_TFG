package com.juan.tfg.service.exception;

public class DuplicateUsernameException extends RuntimeException {

    /**
     * Creates an exception for a username that already exists.
     *
     * @param message the exception detail message.
     */
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
