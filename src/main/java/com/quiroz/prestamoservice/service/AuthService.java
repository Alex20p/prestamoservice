package com.quiroz.prestamoservice.service;

import com.quiroz.prestamoservice.dto.AuthResponse;
import com.quiroz.prestamoservice.dto.LoginRequest;
import com.quiroz.prestamoservice.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}