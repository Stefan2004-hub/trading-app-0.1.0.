package com.trading.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @Test
    void noResourceFoundMapsToNotFoundResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/does-not-exist");
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/does-not-exist");

        ResponseEntity<ApiErrorResponse> response = handler.handleNoResourceFound(ex, request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Resource not found", response.getBody().message());
        assertEquals("/does-not-exist", response.getBody().path());
    }
}
