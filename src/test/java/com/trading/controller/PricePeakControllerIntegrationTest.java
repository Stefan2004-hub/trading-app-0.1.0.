package com.trading.controller;

import com.trading.dto.lookup.PricePeakResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class PricePeakControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PricePeakService pricePeakService;

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
    private UserPreferenceService userPreferenceService;

    @Test
    void pricePeaksEndpointsReturnExpectedStatusAndPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID pricePeakId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID lastBuyTxId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        PricePeakResponse row = new PricePeakResponse(
            pricePeakId,
            userId,
            assetId,
            "MATIC",
            "Polygon",
            lastBuyTxId,
            new BigDecimal("0.884"),
            OffsetDateTime.parse("2026-02-22T00:29:10.940+02:00"),
            Boolean.TRUE,
            OffsetDateTime.parse("2026-02-22T00:29:10.962+02:00"),
            OffsetDateTime.parse("2026-02-22T00:29:10.962+02:00")
        );

        when(pricePeakService.list(userId, null)).thenReturn(List.of(row));
        when(pricePeakService.update(eq(userId), eq(pricePeakId), any())).thenReturn(row);

        mockMvc.perform(get("/api/price-peaks").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(pricePeakId.toString()))
            .andExpect(jsonPath("$[0].assetSymbol").value("MATIC"))
            .andExpect(jsonPath("$[0].active").value(true));
        verify(pricePeakService).list(eq(userId), eq(null));

        mockMvc.perform(
                put("/api/price-peaks/{id}", pricePeakId)
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "peakPrice": 0.8123,
                          "peakTimestamp": "2026-02-25T10:15:00Z",
                          "active": false
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(pricePeakId.toString()))
            .andExpect(jsonPath("$.assetName").value("Polygon"));
        verify(pricePeakService).update(eq(userId), eq(pricePeakId), any());

        mockMvc.perform(delete("/api/price-peaks/{id}", pricePeakId).with(authentication(auth)))
            .andExpect(status().isNoContent());
        verify(pricePeakService).delete(eq(userId), eq(pricePeakId));
    }

    @Test
    void pricePeaksEndpointsRequireAuthentication() throws Exception {
        UUID pricePeakId = UUID.randomUUID();
        mockMvc.perform(get("/api/price-peaks"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(
                put("/api/price-peaks/{id}", pricePeakId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "peakPrice": 0.8123,
                          "peakTimestamp": "2026-02-25T10:15:00Z",
                          "active": false
                        }
                        """)
            )
            .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/price-peaks/{id}", pricePeakId))
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
