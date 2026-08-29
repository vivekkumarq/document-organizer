package com.vivek.docorganizer.exception;

/** Domain exception mapped to HTTP 400 by the global exception handler. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
