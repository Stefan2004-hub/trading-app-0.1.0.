package com.trading.service.auth;

import com.trading.dto.auth.RegisterRequest;
import com.trading.dto.auth.RegisterResponse;

public interface AuthRegistrationService {

    RegisterResponse register(RegisterRequest request);
}
