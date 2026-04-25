package com.sarinkejohn.digitalwalletbackendservice.service;

import com.sarinkejohn.digitalwalletbackendservice.dto.AuthResponse;
import com.sarinkejohn.digitalwalletbackendservice.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(String username, String password, String channel);
    AuthResponse registerAdmin(RegisterRequest request);
}