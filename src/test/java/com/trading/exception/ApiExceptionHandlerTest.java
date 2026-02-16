package com.trading.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void oauthEmailConflictMapsToConflictResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/callback");
        OAuthEmailConflictException ex = new OAuthEmailConflictException();

        ResponseEntity<ApiErrorResponse> response = handler.handleOAuthEmailConflict(ex, request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals(ex.getMessage(), response.getBody().message());
        assertEquals("/api/auth/oauth2/callback", response.getBody().path());
    }
}
