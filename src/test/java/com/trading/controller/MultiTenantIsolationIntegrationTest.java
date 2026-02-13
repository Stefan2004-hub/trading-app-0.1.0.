package com.trading.controller;

import com.trading.domain.enums.StrategyAlertStatus;
import com.trading.domain.enums.StrategyType;
import com.trading.domain.enums.TransactionType;
import com.trading.dto.strategy.StrategyAlertResponse;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.security.UserPrincipal;
import com.trading.service.lookup.LookupService;
import com.trading.service.portfolio.PortfolioService;
import com.trading.service.strategy.BuyStrategyService;
import com.trading.service.strategy.SellStrategyService;
import com.trading.service.strategy.StrategyAlertService;
import com.trading.service.transaction.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
class MultiTenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private SellStrategyService sellStrategyService;

    @MockBean
    private BuyStrategyService buyStrategyService;

    @MockBean
    private StrategyAlertService strategyAlertService;

    @MockBean
    private LookupService lookupService;

    @Test
    void userCannotReadAnotherUsersTransactions() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        Authentication authA = authenticationFor(userA);
        Authentication authB = authenticationFor(userB);

        when(transactionService.list(userA)).thenReturn(List.of(txFor(userA)));
        when(transactionService.list(userB)).thenReturn(List.of(txFor(userB)));

        mockMvc.perform(get("/api/transactions").with(authentication(authA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(userA.toString()));

        mockMvc.perform(get("/api/transactions").with(authentication(authB)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(userB.toString()));

        verify(transactionService).list(eq(userA));
        verify(transactionService).list(eq(userB));
    }

    @Test
    void userCannotAcknowledgeAnotherUsersAlert() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        Authentication authA = authenticationFor(userA);
        Authentication authB = authenticationFor(userB);

        when(strategyAlertService.acknowledge(userA, alertId)).thenReturn(alertFor(userA, alertId));
        when(strategyAlertService.acknowledge(userB, alertId))
            .thenThrow(new IllegalArgumentException("Strategy alert not found: " + alertId));

        mockMvc.perform(post("/api/strategies/alerts/{id}/acknowledge", alertId).with(authentication(authB)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Strategy alert not found: " + alertId));

        mockMvc.perform(post("/api/strategies/alerts/{id}/acknowledge", alertId).with(authentication(authA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userA.toString()))
            .andExpect(jsonPath("$.id").value(alertId.toString()));

        verify(strategyAlertService).acknowledge(eq(userA), eq(alertId));
        verify(strategyAlertService).acknowledge(eq(userB), eq(alertId));
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

    private static TransactionResponse txFor(UUID userId) {
        return new TransactionResponse(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            TransactionType.BUY,
            new BigDecimal("0.3"),
            BigDecimal.ZERO,
            null,
            new BigDecimal("0.3"),
            new BigDecimal("60000"),
            new BigDecimal("18000"),
            null,
            OffsetDateTime.parse("2026-02-13T10:00:00Z")
        );
    }

    private static StrategyAlertResponse alertFor(UUID userId, UUID alertId) {
        return new StrategyAlertResponse(
            alertId,
            userId,
            UUID.randomUUID(),
            StrategyType.SELL,
            new BigDecimal("110000"),
            new BigDecimal("10.0"),
            new BigDecimal("100000"),
            "Sell threshold reached",
            StrategyAlertStatus.ACKNOWLEDGED,
            OffsetDateTime.parse("2026-02-13T09:00:00Z"),
            OffsetDateTime.parse("2026-02-13T09:05:00Z"),
            null
        );
    }
}
