package com.vivek.docorganizer.exception;

/**
 * Raised when a login attempt fails. Mapped to HTTP 401.
 *
 * <p>The message is deliberately identical whether the account is unknown or the password is
 * wrong, so the endpoint cannot be used to enumerate registered email addresses.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
