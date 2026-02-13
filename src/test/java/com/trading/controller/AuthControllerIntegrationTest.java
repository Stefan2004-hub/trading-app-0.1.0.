package com.trading.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.security.UserPrincipal;
import com.trading.service.transaction.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    void registerLoginRefreshLogoutAndMeEndpointsPass() throws Exception {
        mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "email": "trader@example.com",
                          "username": "satoshi",
                          "password": "Secret123!"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("trader@example.com"))
            .andExpect(jsonPath("$.username").value("satoshi"))
            .andExpect(jsonPath("$.authProvider").value("LOCAL"));

        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "identifier": "satoshi",
                          "password": "Secret123!"
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").exists())
            .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
            .andExpect(jsonPath("$.refreshToken").value(not(blankOrNullString())))
            .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        UUID userId = UUID.fromString(loginBody.get("userId").asText());
        String refreshToken = loginBody.get("refreshToken").asText();
        Authentication auth = authenticationFor(userId);

        MvcResult refreshResult = mockMvc.perform(
                post("/api/auth/refresh")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(refreshToken))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value(not(blankOrNullString())))
            .andExpect(jsonPath("$.refreshToken").value(not(blankOrNullString())))
            .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedRefreshToken = refreshBody.get("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.email").value("trader@example.com"))
            .andExpect(jsonPath("$.username").value("satoshi"))
            .andExpect(jsonPath("$.authProvider").value("LOCAL"));

        mockMvc.perform(
                post("/api/auth/logout")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "refreshToken": "%s"
                        }
                        """.formatted(rotatedRefreshToken))
            )
            .andExpect(status().isNoContent());
    }

    @Test
    void meEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    private static Authentication authenticationFor(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
            userId,
            "trader@example.com",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
