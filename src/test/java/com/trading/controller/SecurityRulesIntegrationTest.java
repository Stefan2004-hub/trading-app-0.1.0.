package com.trading.controller;

import com.trading.security.UserPrincipal;
import com.trading.service.lookup.AssetService;
import com.trading.service.lookup.ExchangeService;
import com.trading.service.lookup.LookupService;
import com.trading.service.lookup.PricePeakService;
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

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class SecurityRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private AssetService assetService;

    @MockBean
    private ExchangeService exchangeService;

    @MockBean
    private LookupService lookupService;

    @MockBean
    private PricePeakService pricePeakService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/transactions/ping"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/assets"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRejectAuthenticatedUsersWithoutRoleUser() throws Exception {
        Authentication authWithoutRole = authenticationWithoutUserRole(UUID.randomUUID());

        mockMvc.perform(get("/api/transactions/ping").with(authentication(authWithoutRole)))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/assets").with(authentication(authWithoutRole)))
            .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointsAllowRoleUser() throws Exception {
        Authentication auth = authenticationWithUserRole(UUID.randomUUID());

        mockMvc.perform(get("/api/transactions/ping").with(authentication(auth)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/assets").with(authentication(auth)))
            .andExpect(status().isOk());
    }

    @Test
    void authPingIsPublic() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
            .andExpect(status().isOk());
    }

    private static Authentication authenticationWithUserRole(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
            userId,
            "trader@example.com",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private static Authentication authenticationWithoutUserRole(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
            userId,
            "trader@example.com",
            "N/A",
            List.of()
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
