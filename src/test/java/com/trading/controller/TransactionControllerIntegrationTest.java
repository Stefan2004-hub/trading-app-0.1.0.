package com.trading.controller;

import com.trading.service.backup.BackupService;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.TransactionListView;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.enums.TransactionAccumulationRole;
import com.trading.dto.transaction.AccumulationTradeAssetSummaryResponse;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CleanHistoryBackup;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.security.UserPrincipal;
import com.trading.service.lookup.AssetService;
import com.trading.service.lookup.ExchangeService;
import com.trading.service.lookup.LookupService;
import com.trading.service.lookup.PricePeakService;
import com.trading.service.portfolio.PortfolioService;
import com.trading.service.strategy.BuyStrategyService;
import com.trading.service.strategy.SellStrategyService;
import com.trading.service.strategy.StrategyAlertService;
import com.trading.service.marketalert.MarketAlertService;
import com.trading.service.transaction.AccumulationTradeService;
import com.trading.service.transaction.TransactionService;
import com.trading.service.user.UserPreferenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class TransactionControllerIntegrationTest {

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

    @Test
    void listEndpointReturnsExpectedPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        TransactionResponse tx = txResponse(userId, TransactionType.BUY);
        when(transactionService.list(eq(userId), eq(0), eq(20), eq(null), eq(TransactionListView.OPEN), eq(20), eq(null), eq(null), eq(null), eq(null)))
            .thenReturn(pageOf(List.of(tx), 0, 20, 1));

        mockMvc.perform(get("/api/transactions").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(tx.id().toString()))
            .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
            .andExpect(jsonPath("$.content[0].transactionType").value("BUY"))
            .andExpect(jsonPath("$.content[0].grossAmount").value(0.5))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));

        verify(transactionService).list(eq(userId), eq(0), eq(20), eq(null), eq(TransactionListView.OPEN), eq(20), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void listEndpointPassesSearchParameter() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        when(transactionService.list(eq(userId), eq(2), eq(50), eq("btc"), eq(TransactionListView.OPEN), eq(50), eq(null), eq(null), eq(null), eq(null)))
            .thenReturn(pageOf(List.of(), 2, 50, 0));

        mockMvc.perform(
                get("/api/transactions")
                    .queryParam("page", "2")
                    .queryParam("size", "50")
                    .queryParam("search", "btc")
                    .with(authentication(auth))
            )
            .andExpect(status().isOk());

        verify(transactionService).list(eq(userId), eq(2), eq(50), eq("btc"), eq(TransactionListView.OPEN), eq(50), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void listEndpointPassesMatchedViewAndGroupSize() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        when(transactionService.list(eq(userId), eq(1), eq(20), eq("btc"), eq(TransactionListView.MATCHED), eq(7), eq(null), eq(null), eq(null), eq(null)))
            .thenReturn(pageOf(List.of(), 1, 7, 0));

        mockMvc.perform(
                get("/api/transactions")
                    .queryParam("page", "1")
                    .queryParam("size", "20")
                    .queryParam("search", "btc")
                    .queryParam("view", "MATCHED")
                    .queryParam("groupSize", "7")
                    .with(authentication(auth))
            )
            .andExpect(status().isOk());

        verify(transactionService).list(eq(userId), eq(1), eq(20), eq("btc"), eq(TransactionListView.MATCHED), eq(7), eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void listEndpointPassesSortAndDateParams() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        OffsetDateTime dateFromInclusive = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime dateToExclusive = OffsetDateTime.parse("2027-01-01T00:00:00Z");
        when(transactionService.list(eq(userId), eq(0), eq(20), eq(null), eq(TransactionListView.OPEN), eq(20), eq("date"), eq("asc"), eq(dateFromInclusive), eq(dateToExclusive)))
            .thenReturn(pageOf(List.of(), 0, 20, 0));

        mockMvc.perform(
                get("/api/transactions")
                    .queryParam("sortBy", "date")
                    .queryParam("sortDirection", "asc")
                    .queryParam("dateFromInclusive", "2026-01-01T00:00:00Z")
                    .queryParam("dateToExclusive", "2027-01-01T00:00:00Z")
                    .with(authentication(auth))
            )
            .andExpect(status().isOk());

        verify(transactionService).list(eq(userId), eq(0), eq(20), eq(null), eq(TransactionListView.OPEN), eq(20), eq("date"), eq("asc"), eq(dateFromInclusive), eq(dateToExclusive));
    }

    @Test
    void listEndpointRejectsInvalidSortBy() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        when(transactionService.list(eq(userId), eq(0), eq(20), eq(null), eq(TransactionListView.OPEN), eq(20), eq("asset"), eq(null), eq(null), eq(null)))
            .thenThrow(new IllegalArgumentException("sortBy must be 'date'"));

        mockMvc.perform(
                get("/api/transactions")
                    .queryParam("sortBy", "asset")
                    .with(authentication(auth))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("sortBy must be 'date'"));
    }

    @Test
    void listEndpointRejectsInvalidSortDirection() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        when(transactionService.list(eq(userId), eq(0), eq(20), eq(null), eq(TransactionListView.OPEN), eq(20), eq("date"), eq("sideways"), eq(null), eq(null)))
            .thenThrow(new IllegalArgumentException("sortDirection must be 'asc' or 'desc'"));

        mockMvc.perform(
                get("/api/transactions")
                    .queryParam("sortBy", "date")
                    .queryParam("sortDirection", "sideways")
                    .with(authentication(auth))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("sortDirection must be 'asc' or 'desc'"));
    }

    @Test
    void buyEndpointReturnsCreatedAndPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        TransactionResponse response = txResponse(userId, TransactionType.BUY);
        when(transactionService.buy(eq(userId), org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions/buy")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "assetId": "%s",
                          "exchangeId": "%s",
                          "grossAmount": 0.5,
                          "inputMode": "COIN_AMOUNT",
                          "feeAmount": 10.0,
                          "feeCurrency": "USD",
                          "unitPriceUsd": 100000.0,
                          "transactionDate": "2026-02-13T10:00:00Z"
                        }
                        """.formatted(assetId, exchangeId))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(response.id().toString()))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.transactionType").value("BUY"));

        verify(transactionService).buy(eq(userId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sellEndpointReturnsCreatedAndPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        UUID exchangeId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        TransactionResponse response = txResponse(userId, TransactionType.SELL);
        when(transactionService.sell(eq(userId), org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(
                post("/api/transactions/sell")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "assetId": "%s",
                          "exchangeId": "%s",
                          "grossAmount": 0.2,
                          "feeAmount": 5.0,
                          "feeCurrency": "USD",
                          "unitPriceUsd": 120000.0,
                          "transactionDate": "2026-02-13T10:00:00Z"
                        }
                        """.formatted(assetId, exchangeId))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(response.id().toString()))
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.transactionType").value("SELL"));

        verify(transactionService).sell(eq(userId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cleanHistoryEndpointReturnsExcelAttachment() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);
        CleanHistoryBackup backup = new CleanHistoryBackup(
            "excel-content".getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "trading-history-backup-20260222-120000.xlsx"
        );
        when(transactionService.cleanHistory(userId)).thenReturn(backup);

        mockMvc.perform(
                post("/api/transactions/clean-history")
                    .with(authentication(auth))
            )
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(
                "Content-Type",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(
                "Content-Disposition",
                "attachment; filename=\"" + backup.fileName() + "\""
            ));

        verify(transactionService).cleanHistory(eq(userId));
    }

    @Test
    void updateNetAmountEndpointReturnsUpdatedPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        TransactionResponse response = txResponse(userId, TransactionType.BUY);
        when(transactionService.updateTransactionNetAmount(eq(userId), eq(transactionId), org.mockito.ArgumentMatchers.any()))
            .thenReturn(response);

        mockMvc.perform(
                patch("/api/transactions/{transactionId}/net-amount", transactionId)
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "netAmount": 0.42
                        }
                        """)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id().toString()))
            .andExpect(jsonPath("$.userId").value(userId.toString()));

        verify(transactionService).updateTransactionNetAmount(eq(userId), eq(transactionId), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void accumulationTradeListEndpointReturnsPagedPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);
        AccumulationTradeResponse response = accumulationTradeResponse(userId, assetId);

        when(accumulationTradeService.list(userId, 2, 10, AccumulationTradeStatus.CLOSED, assetId))
            .thenReturn(accumulationPageOf(List.of(response), 2, 10, 21));

        mockMvc.perform(
                get("/api/accumulation-trades")
                    .queryParam("page", "2")
                    .queryParam("size", "10")
                    .queryParam("status", "CLOSED")
                    .queryParam("assetId", assetId.toString())
                    .with(authentication(auth))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(response.id().toString()))
            .andExpect(jsonPath("$.content[0].assetId").value(assetId.toString()))
            .andExpect(jsonPath("$.content[0].status").value("CLOSED"))
            .andExpect(jsonPath("$.totalElements").value(21))
            .andExpect(jsonPath("$.totalPages").value(3));

        verify(accumulationTradeService).list(userId, 2, 10, AccumulationTradeStatus.CLOSED, assetId);
    }

    @Test
    void accumulationTradeListRejectsMismatchedUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);

        mockMvc.perform(
                get("/api/accumulation-trades")
                    .queryParam("userId", otherUserId.toString())
                    .with(authentication(auth))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void accumulationTradeGroupedByAssetEndpointReturnsTotals() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID assetId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);
        AccumulationTradeAssetSummaryResponse summary = new AccumulationTradeAssetSummaryResponse(
            assetId,
            new BigDecimal("0.250000000000000000"),
            2
        );

        when(accumulationTradeService.summarizeByAsset(userId, AccumulationTradeStatus.CLOSED, assetId))
            .thenReturn(List.of(summary));

        mockMvc.perform(
                get("/api/accumulation-trades/grouped-by-asset")
                    .queryParam("status", "CLOSED")
                    .queryParam("assetId", assetId.toString())
                    .with(authentication(auth))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].assetId").value(assetId.toString()))
            .andExpect(jsonPath("$[0].totalAccumulationDelta").value(0.25))
            .andExpect(jsonPath("$[0].tradeCount").value(2));

        verify(accumulationTradeService).summarizeByAsset(userId, AccumulationTradeStatus.CLOSED, assetId);
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/transactions/buy").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/transactions/sell").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/accumulation-trades"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidBuyPayloadReturnsStructuredValidationError() throws Exception {
        Authentication auth = authenticationFor(UUID.randomUUID());

        mockMvc.perform(
                post("/api/transactions/buy")
                    .with(authentication(auth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "grossAmount": 0,
                          "unitPriceUsd": 0
                        }
                        """)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.path").value("/api/transactions/buy"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.violations[*].field", hasItem("assetId")))
            .andExpect(jsonPath("$.violations[*].field", hasItem("exchangeId")))
            .andExpect(jsonPath("$.violations[*].field", hasItem("inputMode")))
            .andExpect(jsonPath("$.violations[*].field", hasItem("unitPriceUsd")));
    }
    @MockBean
    private BackupService backupService;


    private static Authentication authenticationFor(UUID userId) {
        UserPrincipal principal = new UserPrincipal(
            userId,
            "trader@example.com",
            "N/A",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private static TransactionResponse txResponse(UUID userId, TransactionType transactionType) {
        return new TransactionResponse(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            transactionType,
            new BigDecimal("0.5"),
            new BigDecimal("10"),
            new BigDecimal("0.02"),
            "USD",
            new BigDecimal("0.5"),
            new BigDecimal("100000"),
            new BigDecimal("50010"),
            transactionType == TransactionType.SELL ? new BigDecimal("500") : null,
            OffsetDateTime.parse("2026-02-13T10:00:00Z"),
            false,
            null,
            false,
            TransactionAccumulationRole.NONE
        );
    }

    private static Page<TransactionResponse> pageOf(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements
    ) {
        return new PageImpl<>(
            content,
            org.springframework.data.domain.PageRequest.of(page, size),
            totalElements
        );
    }

    private static Page<AccumulationTradeResponse> accumulationPageOf(
        List<AccumulationTradeResponse> content,
        int page,
        int size,
        long totalElements
    ) {
        return new PageImpl<>(
            content,
            org.springframework.data.domain.PageRequest.of(page, size),
            totalElements
        );
    }

    private static AccumulationTradeResponse accumulationTradeResponse(UUID userId, UUID assetId) {
        return new AccumulationTradeResponse(
            UUID.randomUUID(),
            userId,
            assetId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("1.000000000000000000"),
            new BigDecimal("1.250000000000000000"),
            new BigDecimal("0.250000000000000000"),
            AccumulationTradeStatus.CLOSED,
            new BigDecimal("60000.000000000000000000"),
            new BigDecimal("50000.000000000000000000"),
            OffsetDateTime.parse("2026-02-13T10:00:00Z"),
            OffsetDateTime.parse("2026-02-14T10:00:00Z"),
            null
        );
    }
}
