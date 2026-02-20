package com.trading.controller;

import com.trading.domain.enums.StrategyAlertStatus;
import com.trading.domain.enums.StrategyType;
import com.trading.dto.strategy.BuyStrategyResponse;
import com.trading.dto.strategy.SellStrategyResponse;
import com.trading.dto.strategy.StrategyAlertResponse;
import com.trading.security.UserPrincipal;
import com.trading.service.lookup.AssetService;
import com.trading.service.lookup.ExchangeService;
import com.trading.service.lookup.LookupService;
import com.trading.service.portfolio.PortfolioService;
import com.trading.service.strategy.BuyStrategyService;
import com.trading.service.strategy.SellStrategyService;
import com.trading.service.strategy.StrategyAlertService;
import com.trading.service.transaction.TransactionService;
import com.trading.service.user.UserPreferenceService;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
class StrategyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SellStrategyService sellStrategyService;
    @MockBean
    private BuyStrategyService buyStrategyService;
    @MockBean
    private StrategyAlertService strategyAlertService;
    @MockBean
    private TransactionService transactionService;
    @MockBean
    private PortfolioService portfolioService;
    @MockBean
    private AssetService assetService;

    @MockBean
    private ExchangeService exchangeService;

    @MockBean
    private LookupService lookupService;
    @MockBean
    private UserPreferenceService userPreferenceService;

    @Test
    void strategyEndpointsReturnExpectedStatusAndPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        SellStrategyResponse sellResponse = new SellStrategyResponse(
            UUID.randomUUID(),
            userId,
            assetId,
            new BigDecimal("8.50"),
            Boolean.TRUE,
            OffsetDateTime.parse("2026-02-13T10:00:00Z"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );
        BuyStrategyResponse buyResponse = new BuyStrategyResponse(
            UUID.randomUUID(),
            userId,
            assetId,
            new BigDecimal("10.00"),
            new BigDecimal("250.00"),
            Boolean.TRUE,
            OffsetDateTime.parse("2026-02-13T10:00:00Z"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );
        StrategyAlertResponse pendingAlert = new StrategyAlertResponse(
            alertId,
            userId,
            assetId,
            StrategyType.SELL,
            new BigDecimal("105000"),
            new BigDecimal("8.50"),
            new BigDecimal("97000"),
            "Sell threshold reached for BTC",
            StrategyAlertStatus.PENDING,
            OffsetDateTime.parse("2026-02-13T09:00:00Z"),
            null,
            null
        );
        StrategyAlertResponse acknowledgedAlert = new StrategyAlertResponse(
            alertId,
            userId,
            assetId,
            StrategyType.SELL,
            new BigDecimal("105000"),
            new BigDecimal("8.50"),
            new BigDecimal("97000"),
            "Sell threshold reached for BTC",
            StrategyAlertStatus.ACKNOWLEDGED,
            OffsetDateTime.parse("2026-02-13T09:00:00Z"),
            OffsetDateTime.parse("2026-02-13T09:05:00Z"),
            null
        );

        when(sellStrategyService.list(userId)).thenReturn(List.of(sellResponse));
        when(sellStrategyService.upsert(eq(userId), any())).thenReturn(sellResponse);
        when(buyStrategyService.list(userId)).thenReturn(List.of(buyResponse));
        when(buyStrategyService.upsert(eq(userId), any())).thenReturn(buyResponse);
        when(strategyAlertService.list(userId)).thenReturn(List.of(pendingAlert));
        when(strategyAlertService.acknowledge(userId, alertId)).thenReturn(acknowledgedAlert);

        mockMvc.perform(get("/api/strategies/sell").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(userId.toString()))
            .andExpect(jsonPath("$[0].thresholdPercent").value(8.5));
        verify(sellStrategyService).list(eq(userId));

        mockMvc.perform(
                post("/api/strategies/sell")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "assetId": "%s",
                          "thresholdPercent": 8.5,
                          "active": true
                        }
                        """.formatted(assetId))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assetId").value(assetId.toString()))
            .andExpect(jsonPath("$.thresholdPercent").value(8.5));
        verify(sellStrategyService).upsert(eq(userId), any());

        mockMvc.perform(get("/api/strategies/buy").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(userId.toString()))
            .andExpect(jsonPath("$[0].dipThresholdPercent").value(10.0));
        verify(buyStrategyService).list(eq(userId));

        mockMvc.perform(
                post("/api/strategies/buy")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "assetId": "%s",
                          "dipThresholdPercent": 10.0,
                          "buyAmountUsd": 250.0,
                          "active": true
                        }
                        """.formatted(assetId))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.assetId").value(assetId.toString()))
            .andExpect(jsonPath("$.buyAmountUsd").value(250.0));
        verify(buyStrategyService).upsert(eq(userId), any());

        mockMvc.perform(get("/api/strategies/alerts").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(alertId.toString()))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
        verify(strategyAlertService).list(eq(userId));

        mockMvc.perform(post("/api/strategies/alerts/{id}/acknowledge", alertId).with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
            .andExpect(jsonPath("$.acknowledgedAt").value("2026-02-13T09:05:00Z"));
        verify(strategyAlertService).acknowledge(eq(userId), eq(alertId));
    }

    @Test
    void strategyEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/strategies/sell"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/strategies/sell").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/strategies/buy"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/strategies/buy").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/strategies/alerts"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/strategies/alerts/{id}/acknowledge", UUID.randomUUID()))
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
