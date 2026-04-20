package com.sarinkejohn.digitalwalletbackendservice.exception;

public class DuplicateReferenceException extends RuntimeException {
    public DuplicateReferenceException(String message) {
        super(message);
    }
}