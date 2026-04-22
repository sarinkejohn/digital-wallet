package com.sarinkejohn.digitalwalletbackendservice.exception;

public class AccountLockedException extends RuntimeException {
    private final long lockoutSeconds;

    public AccountLockedException(String message, long lockoutSeconds) {
        super(message);
        this.lockoutSeconds = lockoutSeconds;
    }

    public long getLockoutSeconds() {
        return lockoutSeconds;
    }
}