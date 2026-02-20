package com.trading.controller;

import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.ExchangeLookupResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

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
class LookupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetService assetService;

    @MockBean
    private ExchangeService exchangeService;

    @MockBean
    private LookupService lookupService;

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
    private UserPreferenceService userPreferenceService;

    @Test
    void assetsEndpointReturnsSeededStyleList() throws Exception {
        AssetLookupResponse row = new AssetLookupResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "BTC",
            "Bitcoin"
        );
        when(assetService.list(null)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/assets").with(authentication(authenticationFor())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("11111111-1111-1111-1111-111111111111"))
            .andExpect(jsonPath("$[0].symbol").value("BTC"))
            .andExpect(jsonPath("$[0].name").value("Bitcoin"));

        verify(assetService).list(null);
    }

    @Test
    void exchangesEndpointReturnsSeededStyleList() throws Exception {
        ExchangeLookupResponse row = new ExchangeLookupResponse(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            "BINANCE",
            "Binance"
        );
        when(exchangeService.list(null)).thenReturn(List.of(row));

        mockMvc.perform(get("/api/exchanges").with(authentication(authenticationFor())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
            .andExpect(jsonPath("$[0].symbol").value("BINANCE"))
            .andExpect(jsonPath("$[0].name").value("Binance"));

        verify(exchangeService).list(null);
    }

    @Test
    void lookupEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/assets"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/exchanges"))
            .andExpect(status().isUnauthorized());
    }

    private static Authentication authenticationFor() {
        UserPrincipal principal = new UserPrincipal(
            UUID.randomUUID(),
            "trader@example.com",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
