package com.trading.service.auth;

import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.OAuthCallbackRequest;

public interface AuthOAuthService {

    AuthResponse handleGoogleCallback(OAuthCallbackRequest request);
}
