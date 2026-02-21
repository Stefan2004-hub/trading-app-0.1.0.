package com.trading.controller;

import com.trading.dto.portfolio.PortfolioAssetPerformanceResponse;
import com.trading.dto.portfolio.PortfolioSummaryResponse;
import com.trading.security.UserPrincipal;
import com.trading.service.lookup.AssetService;
import com.trading.service.lookup.ExchangeService;
import com.trading.service.lookup.LookupService;
import com.trading.service.portfolio.PortfolioService;
import com.trading.service.strategy.BuyStrategyService;
import com.trading.service.strategy.SellStrategyService;
import com.trading.service.strategy.StrategyAlertService;
import com.trading.service.transaction.AccumulationTradeService;
import com.trading.service.transaction.TransactionService;
import com.trading.service.user.UserPreferenceService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class PortfolioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private TransactionService transactionService;
    
    
    @MockBean
    private AccumulationTradeService accumulationTradeService;

    @MockBean
    private SellStrategyService sellStrategyService;

    @MockBean
    private BuyStrategyService buyStrategyService;

    @MockBean
    private StrategyAlertService strategyAlertService;

    @MockBean
    private AssetService assetService;

    @MockBean
    private ExchangeService exchangeService;

    @MockBean
    private LookupService lookupService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @Test
    void summaryEndpointReturnsUserScopedData() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
            new BigDecimal("64000"),
            new BigDecimal("69600"),
            new BigDecimal("5600"),
            new BigDecimal("8.75"),
            new BigDecimal("1300"),
            new BigDecimal("6900"),
            List.of(assetRow())
        );
        when(portfolioService.getSummary(userId)).thenReturn(summary);

        mockMvc.perform(get("/api/portfolio/summary").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalInvestedUsd").value(64000))
            .andExpect(jsonPath("$.totalCurrentValueUsd").value(69600))
            .andExpect(jsonPath("$.assets[0].symbol").value("BTC"));

        verify(portfolioService).getSummary(eq(userId));
    }

    @Test
    void performanceEndpointReturnsUserScopedData() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        when(portfolioService.getPerformance(userId)).thenReturn(List.of(assetRow()));

        mockMvc.perform(get("/api/portfolio/performance").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].symbol").value("BTC"))
            .andExpect(jsonPath("$[0].currentBalance").value(1.2))
            .andExpect(jsonPath("$[0].unrealizedPnlUsd").value(6000));

        verify(portfolioService).getPerformance(eq(userId));
    }

    @Test
    void portfolioEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/portfolio/summary"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/portfolio/performance"))
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

    private static PortfolioAssetPerformanceResponse assetRow() {
        return new PortfolioAssetPerformanceResponse(
            "BTC",
            "Binance",
            new BigDecimal("1.2"),
            new BigDecimal("60000"),
            new BigDecimal("50000"),
            new BigDecimal("55000"),
            new BigDecimal("66000"),
            new BigDecimal("6000"),
            new BigDecimal("10.0"),
            new BigDecimal("1500"),
            new BigDecimal("7500")
        );
    }
}
