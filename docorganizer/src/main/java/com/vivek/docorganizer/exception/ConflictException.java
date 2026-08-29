package com.vivek.docorganizer.exception;

/** Domain exception mapped to HTTP 409 by the global exception handler. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
