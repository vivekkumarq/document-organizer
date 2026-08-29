package com.vivek.docorganizer.exception;

/** Raised when an upload would push a user past their storage quota. Mapped to HTTP 413. */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
