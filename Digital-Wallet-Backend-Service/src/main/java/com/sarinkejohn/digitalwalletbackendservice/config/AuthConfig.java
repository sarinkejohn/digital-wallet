package com.sarinkejohn.digitalwalletbackendservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthConfig {

    private int maxLoginAttempts = 5;
    private int lockoutDurationMinutes = 5;
    private int bcryptRounds = 10;

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public int getBcryptRounds() {
        return bcryptRounds;
    }

    public void setBcryptRounds(int bcryptRounds) {
        this.bcryptRounds = bcryptRounds;
    }
}