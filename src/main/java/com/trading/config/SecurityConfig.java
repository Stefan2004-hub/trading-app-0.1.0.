package com.trading.config;

import com.trading.dto.auth.AuthResponse;
import com.trading.dto.auth.OAuthCallbackRequest;
import com.trading.security.JwtAuthenticationFilter;
import com.trading.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        AuthenticationSuccessHandler oauth2SuccessHandler,
        AuthenticationFailureHandler oauth2FailureHandler
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .requestMatchers("/api/**").hasRole("USER")
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth -> oauth
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable())
            .build();
    }

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(
        AuthService authService,
        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        return (request, response, authentication) -> onOauth2Success(authService, frontendBaseUrl, response, authentication);
    }

    @Bean
    AuthenticationFailureHandler oauth2FailureHandler(
        @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        return (request, response, exception) -> {
            String redirectUrl = frontendUrl(frontendBaseUrl, "/login", "oauthError", "Google sign-in failed");
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigin
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void onOauth2Success(
        AuthService authService,
        String frontendBaseUrl,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User oauthUser)) {
            throw new ServletException("Unexpected OAuth2 principal type");
        }

        String email = attributeAsString(oauthUser, "email");
        String providerUserId = attributeAsString(oauthUser, "sub");
        String preferredUsername = attributeAsString(oauthUser, "name");

        AuthResponse authResponse =
            authService.handleGoogleCallback(new OAuthCallbackRequest(email, providerUserId, preferredUsername));

        String redirectUrl = UriComponentsBuilder
            .fromUriString(trimBase(frontendBaseUrl) + "/auth/google/callback")
            .queryParam("userId", authResponse.userId())
            .queryParam("email", authResponse.email())
            .queryParam("username", authResponse.username())
            .queryParam("authProvider", authResponse.authProvider())
            .queryParam("accessToken", authResponse.accessToken())
            .queryParam("refreshToken", authResponse.refreshToken())
            .build()
            .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private static String frontendUrl(String frontendBaseUrl, String path, String key, String value) {
        return UriComponentsBuilder
            .fromUriString(trimBase(frontendBaseUrl) + path)
            .queryParam(key, value)
            .build()
            .toUriString();
    }

    private static String trimBase(String frontendBaseUrl) {
        return frontendBaseUrl.endsWith("/") ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1) : frontendBaseUrl;
    }

    private static String attributeAsString(OAuth2User oauthUser, String attributeKey) throws ServletException {
        Object value = oauthUser.getAttributes().get(attributeKey);
        if (!(value instanceof String strValue) || strValue.isBlank()) {
            throw new ServletException("Missing OAuth2 attribute: " + attributeKey);
        }
        return strValue;
    }
}
