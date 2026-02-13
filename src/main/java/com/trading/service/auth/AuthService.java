package com.trading.service.auth;

import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.LoginRequest;
import com.trading.dto.auth.LoginResponse;
import com.trading.dto.auth.LogoutRequest;
import com.trading.dto.auth.MeResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.dto.auth.RefreshRequest;
import com.trading.dto.auth.RefreshResponse;
import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;

import java.util.UUID;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    AuthResponse handleGoogleCallback(OAuthCallbackRequest request);

    RefreshResponse refresh(UUID userId, RefreshRequest request);

    void logout(UUID userId, LogoutRequest request);

    MeResponse me(UUID userId);
}
