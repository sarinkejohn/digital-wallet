package com.sarinkejohn.digitalwalletbackendservice.service;

import com.sarinkejohn.digitalwalletbackendservice.dto.AuthResponse;
import com.sarinkejohn.digitalwalletbackendservice.dto.RegisterRequest;
import com.sarinkejohn.digitalwalletbackendservice.entity.User;
import com.sarinkejohn.digitalwalletbackendservice.exception.InvalidAmountException;
import com.sarinkejohn.digitalwalletbackendservice.repository.UserRepository;
import com.sarinkejohn.digitalwalletbackendservice.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, WalletService walletService,
                     JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidAmountException("Username already exists");
        }

        walletService.createUserAndWallet(request.getUsername(), 
                passwordEncoder.encode(request.getPassword()), "USER");

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidAmountException("User creation failed"));

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());

        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }

    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidAmountException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidAmountException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole(), user.getId());

        return new AuthResponse(token, user.getUsername(), user.getRole(), user.getId());
    }
}