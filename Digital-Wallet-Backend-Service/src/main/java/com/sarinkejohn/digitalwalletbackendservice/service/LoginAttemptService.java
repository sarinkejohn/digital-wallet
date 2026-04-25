package com.sarinkejohn.digitalwalletbackendservice.service;

public interface LoginAttemptService {
    void recordFailedAttempt(String username);
    void recordSuccess(String username);
    boolean isLocked(String username);
    boolean checkAndUnlock(String username);
    String getLockoutRemaining(String username);
    int getAttemptsRemaining(String username);
}