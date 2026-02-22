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

import java.io.Writer;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class BackupControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void sqlBackupEndpointReturnsSqlAttachment() throws Exception {
        UUID userId = UUID.randomUUID();
        Authentication auth = authenticationFor(userId);
        doAnswer(invocation -> {
            Writer writer = invocation.getArgument(1);
            writer.write("SET REFERENTIAL_INTEGRITY FALSE;\n");
            writer.write("SET REFERENTIAL_INTEGRITY TRUE;\n");
            return null;
        }).when(backupService).writeUserBackupSql(eq(userId), any(Writer.class));

        mockMvc.perform(get("/api/system/sql-backup").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/sql"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"trading-sql-backup-")));

        verify(backupService).writeUserBackupSql(eq(userId), any(Writer.class));
    }

    @Test
    void sqlBackupEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/system/sql-backup"))
            .andExpect(status().isUnauthorized());
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
