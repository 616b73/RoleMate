package com.rolemate.backend.exception;

/**
 * Domain-specific runtime exception for RoleMate operations.
 * Used in services to signal matchmaking or session errors that
 * should be translated into WebSocket ERROR events.
 */
public class RoleMateException extends RuntimeException {

    public RoleMateException(String message) {
        super(message);
    }

    public RoleMateException(String message, Throwable cause) {
        super(message, cause);
    }
}
