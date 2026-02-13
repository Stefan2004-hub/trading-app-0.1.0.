package com.trading.service.auth;

import com.trading.dto.auth.LoginRequest;
import com.trading.dto.auth.LoginResponse;

public interface AuthLoginService {

    LoginResponse login(LoginRequest request);
}
