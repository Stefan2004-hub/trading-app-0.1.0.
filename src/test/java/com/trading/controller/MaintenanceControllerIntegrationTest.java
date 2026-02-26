package com.trading.controller;

import com.trading.security.UserPrincipal;
import com.trading.service.backup.BackupService;
import com.trading.service.historical.HistoricalDataService;
import com.trading.service.lookup.AssetService;
import com.trading.service.lookup.ExchangeService;
import com.trading.service.lookup.LookupService;
import com.trading.service.lookup.PricePeakService;
import com.trading.service.marketalert.MarketAlertService;
import com.trading.service.portfolio.PortfolioService;
import com.trading.service.strategy.BuyStrategyService;
import com.trading.service.strategy.SellStrategyService;
import com.trading.service.strategy.StrategyAlertService;
import com.trading.service.system.SystemMaintenanceService;
import com.trading.service.transaction.AccumulationTradeService;
import com.trading.service.transaction.TransactionService;
import com.trading.service.user.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

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
class MaintenanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemMaintenanceService systemMaintenanceService;

    @MockBean
    private BackupService backupService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private AccumulationTradeService accumulationTradeService;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private SellStrategyService sellStrategyService;

    @MockBean
    private BuyStrategyService buyStrategyService;

    @MockBean
    private StrategyAlertService strategyAlertService;

    @MockBean
    private MarketAlertService marketAlertService;

    @MockBean
    private AssetService assetService;

    @MockBean
    private ExchangeService exchangeService;

    @MockBean
    private LookupService lookupService;

    @MockBean
    private PricePeakService pricePeakService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @MockBean
    private HistoricalDataService historicalDataService;

    @BeforeEach
    void resetKeepAliveState() {
        if (!systemMaintenanceService.isKeepAliveActive()) {
            systemMaintenanceService.toggleKeepAlive();
        }
    }

    @Test
    void publicPingReflectsToggleStateAndIsAccessibleWithoutAuth() throws Exception {
        Authentication auth = authenticationFor(UUID.randomUUID());

        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keepAliveActive").value(true));

        mockMvc.perform(post("/api/admin/toggle-ping").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keepAliveActive").value(false));

        mockMvc.perform(get("/api/public/ping"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.keepAliveActive").value(false));
    }

    @Test
    void adminPingEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/ping-status"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/toggle-ping"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminPingStatusReturnsCurrentStateForAuthenticatedUser() throws Exception {
        Authentication auth = authenticationFor(UUID.randomUUID());

        mockMvc.perform(get("/api/admin/ping-status").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keepAliveActive").value(true));

        mockMvc.perform(post("/api/admin/toggle-ping").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keepAliveActive").value(false));

        mockMvc.perform(get("/api/admin/ping-status").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keepAliveActive").value(false));
    }

    private static Authentication authenticationFor(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
            userId,
            "user@example.com",
            "trader",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
