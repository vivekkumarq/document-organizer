package com.vivek.docorganizer.exception;

/** Domain exception mapped to HTTP 403 by the global exception handler. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
