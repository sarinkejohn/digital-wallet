package com.sarinkejohn.digitalwalletbackendservice.service.impl;

import com.sarinkejohn.digitalwalletbackendservice.config.AuthConfig;
import com.sarinkejohn.digitalwalletbackendservice.service.LoginAttemptService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final AuthConfig authConfig;
    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptServiceImpl(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    @Override
    public void recordFailedAttempt(String username) {
        attempts.compute(username, (k, existing) -> {
            if (existing == null) {
                return new LoginAttempt(1, Instant.now());
            }
            existing.increment();
            return existing;
        });
    }

    @Override
    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    @Override
    public boolean isLocked(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null) {
            return false;
        }

        if (attempt.getCount() >= authConfig.getMaxLoginAttempts()) {
            if (attempt.isExpired(authConfig.getLockoutDurationMinutes())) {
                attempts.remove(username);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean checkAndUnlock(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt != null && attempt.isExpired(authConfig.getLockoutDurationMinutes())) {
            attempts.remove(username);
            return true;
        }
        return false;
    }

    @Override
    public String getLockoutRemaining(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null) {
            return null;
        }
        long secondsRemaining = attempt.getSecondsRemaining(authConfig.getLockoutDurationMinutes());
        return secondsRemaining > 0 ? secondsRemaining + " seconds" : null;
    }

    @Override
    public int getAttemptsRemaining(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null) {
            return authConfig.getMaxLoginAttempts();
        }
        return Math.max(0, authConfig.getMaxLoginAttempts() - attempt.getCount());
    }

    private static class LoginAttempt {
        private int count;
        private Instant firstAttempt;

        public LoginAttempt(int count, Instant firstAttempt) {
            this.count = count;
            this.firstAttempt = firstAttempt;
        }

        public void increment() {
            this.count++;
        }

        public int getCount() {
            return count;
        }

        public boolean isExpired(int lockoutMinutes) {
            return Instant.now().isAfter(firstAttempt.plusSeconds(lockoutMinutes * 60L));
        }

        public long getSecondsRemaining(int lockoutMinutes) {
            Instant expiresAt = firstAttempt.plusSeconds(lockoutMinutes * 60L);
            long seconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(0, seconds);
        }
    }
}