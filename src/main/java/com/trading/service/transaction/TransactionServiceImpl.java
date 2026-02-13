package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnBean({
    TransactionRepository.class,
    UserRepository.class,
    AssetRepository.class,
    ExchangeRepository.class
})
public class TransactionServiceImpl implements TransactionService {

    private static final String USD = "USD";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final ExchangeRepository exchangeRepository;

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        UserRepository userRepository,
        AssetRepository assetRepository,
        ExchangeRepository exchangeRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @Override
    public TransactionResponse buy(UUID userId, BuyTransactionRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        validateRequest(request);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));
        Exchange exchange = exchangeRepository.findById(request.exchangeId())
            .orElseThrow(() -> new IllegalArgumentException("Exchange not found: " + request.exchangeId()));

        BigDecimal feeAmount = normalizeFee(request.feeAmount());
        String normalizedFeeCurrency = normalizeFeeCurrency(request.feeCurrency());

        BigDecimal netAmount = request.grossAmount();
        BigDecimal totalSpentUsd = request.grossAmount().multiply(request.unitPriceUsd());

        if (feeAmount.compareTo(ZERO) > 0) {
            if (USD.equals(normalizedFeeCurrency)) {
                totalSpentUsd = totalSpentUsd.add(feeAmount);
            } else if (asset.getSymbol().equalsIgnoreCase(normalizedFeeCurrency)) {
                netAmount = request.grossAmount().subtract(feeAmount);
                if (netAmount.compareTo(ZERO) <= 0) {
                    throw new IllegalArgumentException("netAmount must be positive after asset-denominated fee");
                }
            } else {
                throw new IllegalArgumentException("Unsupported fee currency: " + request.feeCurrency());
            }
        } else if (normalizedFeeCurrency != null && !normalizedFeeCurrency.isBlank()) {
            throw new IllegalArgumentException("feeCurrency requires feeAmount greater than zero");
        }

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAsset(asset);
        tx.setExchange(exchange);
        tx.setTransactionType(TransactionType.BUY);
        tx.setGrossAmount(request.grossAmount());
        tx.setFeeAmount(feeAmount);
        tx.setFeeCurrency(normalizedFeeCurrency);
        tx.setNetAmount(netAmount);
        tx.setUnitPriceUsd(request.unitPriceUsd());
        tx.setTotalSpentUsd(totalSpentUsd);
        tx.setRealizedPnl(null);
        tx.setTransactionDate(request.transactionDate() == null ? OffsetDateTime.now() : request.transactionDate());

        Transaction saved = transactionRepository.save(tx);
        return new TransactionResponse(
            saved.getId(),
            saved.getUser().getId(),
            saved.getAsset().getId(),
            saved.getExchange().getId(),
            saved.getTransactionType(),
            saved.getGrossAmount(),
            saved.getFeeAmount(),
            saved.getFeeCurrency(),
            saved.getNetAmount(),
            saved.getUnitPriceUsd(),
            saved.getTotalSpentUsd(),
            saved.getRealizedPnl(),
            saved.getTransactionDate()
        );
    }

    private static void validateRequest(BuyTransactionRequest request) {
        Objects.requireNonNull(request.assetId(), "assetId is required");
        Objects.requireNonNull(request.exchangeId(), "exchangeId is required");
        Objects.requireNonNull(request.grossAmount(), "grossAmount is required");
        Objects.requireNonNull(request.unitPriceUsd(), "unitPriceUsd is required");

        if (request.grossAmount().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("grossAmount must be positive");
        }
        if (request.unitPriceUsd().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("unitPriceUsd must be positive");
        }
        if (request.feeAmount() != null && request.feeAmount().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("feeAmount must be zero or positive");
        }
    }

    private static BigDecimal normalizeFee(BigDecimal feeAmount) {
        return feeAmount == null ? ZERO : feeAmount;
    }

    private static String normalizeFeeCurrency(String feeCurrency) {
        if (feeCurrency == null || feeCurrency.isBlank()) {
            return null;
        }
        return feeCurrency.trim().toUpperCase(Locale.ROOT);
    }
}
