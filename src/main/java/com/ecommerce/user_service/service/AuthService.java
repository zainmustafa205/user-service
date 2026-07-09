package com.ecommerce.user_service.service;

import com.ecommerce.user_service.dto.request.LoginRequest;
import com.ecommerce.user_service.dto.request.RegisterRequest;
import com.ecommerce.user_service.dto.response.AuthResponse;
import com.ecommerce.user_service.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);
    UserResponse getCurrentUser(String Email);

    
}