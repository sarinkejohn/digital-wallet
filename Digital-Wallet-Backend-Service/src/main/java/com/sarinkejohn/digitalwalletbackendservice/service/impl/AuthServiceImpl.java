package com.sarinkejohn.digitalwalletbackendservice.service.impl;

import com.sarinkejohn.digitalwalletbackendservice.config.AuthConfig;
import com.sarinkejohn.digitalwalletbackendservice.dto.AuthResponse;
import com.sarinkejohn.digitalwalletbackendservice.dto.RegisterRequest;
import com.sarinkejohn.digitalwalletbackendservice.entity.User;
import com.sarinkejohn.digitalwalletbackendservice.exception.AccountLockedException;
import com.sarinkejohn.digitalwalletbackendservice.exception.InvalidAmountException;
import com.sarinkejohn.digitalwalletbackendservice.mapper.RegisterRequestMapper;
import com.sarinkejohn.digitalwalletbackendservice.repository.UserRepository;
import com.sarinkejohn.digitalwalletbackendservice.security.JwtTokenProvider;
import com.sarinkejohn.digitalwalletbackendservice.service.AuthService;
import com.sarinkejohn.digitalwalletbackendservice.service.LoginAttemptService;
import com.sarinkejohn.digitalwalletbackendservice.service.WalletService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthConfig authConfig;
    private final RegisterRequestMapper registerRequestMapper;

    public AuthServiceImpl(UserRepository userRepository, WalletService walletService,
                          JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttemptService, AuthConfig authConfig,
                          RegisterRequestMapper registerRequestMapper) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.authConfig = authConfig;
        this.registerRequestMapper = registerRequestMapper;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidAmountException("Username already exists");
        }

        // Map RegisterRequest to User entity using mapper
        User user = registerRequestMapper.toEntity(request);
        
        // Set role based on admin key
        String role = "USER";
        if (request.getAdminKey() != null && request.getAdminKey().equals("admin-secret-123")) {
            role = "ADMIN";
        }
        user.setRole(role);
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        String channel = request.getChannel() != null ? request.getChannel() : "WEBP";

        // Create user and wallet using the User entity
        walletService.createUserAndWallet(user);

        // The user object now has generated ID after save
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId(), channel);

        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }

    @Override
    public AuthResponse login(String username, String password, String channel) {
        if (loginAttemptService.isLocked(username)) {
            String remaining = loginAttemptService.getLockoutRemaining(username);
            throw new AccountLockedException(
                    "Account locked due to failed login attempts. Try again in " + remaining,
                    remaining != null ? Long.parseLong(remaining.split(" ")[0]) : 300);
        }

        loginAttemptService.checkAndUnlock(username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidAmountException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            loginAttemptService.recordFailedAttempt(username);
            int remaining = loginAttemptService.getAttemptsRemaining(username);
            if (remaining <= 0) {
                throw new AccountLockedException(
                        "Account locked due to " + authConfig.getMaxLoginAttempts() + " failed login attempts. Wait " +
                        authConfig.getLockoutDurationMinutes() + " minutes.",
                        authConfig.getLockoutDurationMinutes() * 60L);
            }
            throw new InvalidAmountException("Invalid credentials. " + remaining + " attempts remaining.");
        }

        loginAttemptService.recordSuccess(username);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId(), channel);

        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }

    @Override
    public AuthResponse registerAdmin(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidAmountException("Username already exists");
        }

        // Map RegisterRequest to User entity using mapper
        User user = registerRequestMapper.toEntity(request);
        
        // Set role to ADMIN
        user.setRole("ADMIN");
        
        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        String channel = request.getChannel() != null ? request.getChannel() : "WEBP";

        // Create user and wallet using the User entity
        walletService.createUserAndWallet(user);

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId(), channel);

        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }
}