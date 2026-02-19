package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.BuyInputMode;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final String USD = "USD";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int DIVISION_SCALE = 18;
    private static final int FEE_PERCENTAGE_SCALE = 6;

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
    public List<TransactionResponse> list(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        return transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId).stream()
            .map(TransactionServiceImpl::toResponse)
            .toList();
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

        BigDecimal grossAmount = resolveBuyGrossAmount(request);
        FeeState feeState = resolveFeeState(
            grossAmount,
            asset.getSymbol(),
            request.feeAmount(),
            request.feePercentage(),
            request.feeCurrency()
        );

        BigDecimal netAmount = grossAmount;
        BigDecimal totalSpentUsd = grossAmount.multiply(request.unitPriceUsd());

        if (feeState.feeAmount().compareTo(ZERO) > 0) {
            if (USD.equals(feeState.feeCurrency())) {
                totalSpentUsd = totalSpentUsd.add(feeState.feeAmount());
            } else if (asset.getSymbol().equalsIgnoreCase(feeState.feeCurrency())) {
                netAmount = grossAmount.subtract(feeState.feeAmount());
                if (netAmount.compareTo(ZERO) <= 0) {
                    throw new IllegalArgumentException("netAmount must be positive after asset-denominated fee");
                }
            } else {
                throw new IllegalArgumentException("Unsupported fee currency: " + request.feeCurrency());
            }
        } else if (feeState.feeCurrency() != null && !feeState.feeCurrency().isBlank()) {
            throw new IllegalArgumentException("feeCurrency requires feeAmount greater than zero");
        }

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAsset(asset);
        tx.setExchange(exchange);
        tx.setTransactionType(TransactionType.BUY);
        tx.setGrossAmount(grossAmount);
        tx.setFeeAmount(feeState.feeAmount());
        tx.setFeePercentage(feeState.feePercentage());
        tx.setFeeCurrency(feeState.feeCurrency());
        tx.setNetAmount(netAmount);
        tx.setUnitPriceUsd(request.unitPriceUsd());
        tx.setTotalSpentUsd(totalSpentUsd);
        tx.setRealizedPnl(null);
        tx.setTransactionDate(request.transactionDate() == null ? OffsetDateTime.now() : request.transactionDate());

        Transaction saved = transactionRepository.save(tx);
        return toResponse(saved);
    }

    @Override
    public TransactionResponse sell(UUID userId, SellTransactionRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        validateRequest(request);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));
        Exchange exchange = exchangeRepository.findById(request.exchangeId())
            .orElseThrow(() -> new IllegalArgumentException("Exchange not found: " + request.exchangeId()));

        FeeState feeState = resolveFeeState(
            request.grossAmount(),
            asset.getSymbol(),
            request.feeAmount(),
            request.feePercentage(),
            request.feeCurrency()
        );

        PositionState currentPosition = calculateCurrentPosition(userId, asset.getId());
        if (currentPosition.quantity().compareTo(request.grossAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient asset balance for sell transaction");
        }

        BigDecimal proceedsUsd = request.grossAmount().multiply(request.unitPriceUsd());
        BigDecimal netAmount = request.grossAmount();
        if (feeState.feeAmount().compareTo(ZERO) > 0) {
            if (USD.equals(feeState.feeCurrency())) {
                proceedsUsd = proceedsUsd.subtract(feeState.feeAmount());
            } else if (asset.getSymbol().equalsIgnoreCase(feeState.feeCurrency())) {
                netAmount = request.grossAmount().subtract(feeState.feeAmount());
                if (netAmount.compareTo(ZERO) <= 0) {
                    throw new IllegalArgumentException("netAmount must be positive after asset-denominated fee");
                }
            } else {
                throw new IllegalArgumentException("Unsupported fee currency: " + request.feeCurrency());
            }
        } else if (feeState.feeCurrency() != null && !feeState.feeCurrency().isBlank()) {
            throw new IllegalArgumentException("feeCurrency requires feeAmount greater than zero");
        }

        if (proceedsUsd.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("total sell proceeds cannot be negative");
        }

        BigDecimal avgCostPerUnit = currentPosition.quantity().compareTo(ZERO) == 0
            ? ZERO
            : currentPosition.costBasisUsd().divide(
                currentPosition.quantity(),
                18,
                java.math.RoundingMode.HALF_UP
            );
        BigDecimal soldCostBasisUsd = avgCostPerUnit.multiply(request.grossAmount());
        BigDecimal realizedPnl = proceedsUsd.subtract(soldCostBasisUsd);

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAsset(asset);
        tx.setExchange(exchange);
        tx.setTransactionType(TransactionType.SELL);
        tx.setGrossAmount(request.grossAmount());
        tx.setFeeAmount(feeState.feeAmount());
        tx.setFeePercentage(feeState.feePercentage());
        tx.setFeeCurrency(feeState.feeCurrency());
        tx.setNetAmount(netAmount);
        tx.setUnitPriceUsd(request.unitPriceUsd());
        tx.setTotalSpentUsd(proceedsUsd);
        tx.setRealizedPnl(realizedPnl);
        tx.setTransactionDate(request.transactionDate() == null ? OffsetDateTime.now() : request.transactionDate());

        Transaction saved = transactionRepository.save(tx);
        return toResponse(saved);
    }

    private static void validateRequest(BuyTransactionRequest request) {
        Objects.requireNonNull(request.assetId(), "assetId is required");
        Objects.requireNonNull(request.exchangeId(), "exchangeId is required");
        Objects.requireNonNull(request.inputMode(), "inputMode is required");
        Objects.requireNonNull(request.unitPriceUsd(), "unitPriceUsd is required");

        if (request.inputMode() == BuyInputMode.COIN_AMOUNT) {
            Objects.requireNonNull(request.grossAmount(), "grossAmount is required when inputMode is COIN_AMOUNT");
            if (request.grossAmount().compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException("grossAmount must be positive");
            }
        } else {
            Objects.requireNonNull(request.usdAmount(), "usdAmount is required when inputMode is USD_AMOUNT");
            if (request.usdAmount().compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException("usdAmount must be positive");
            }
        }
        if (request.unitPriceUsd().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("unitPriceUsd must be positive");
        }
        validateFeeFields(request.feeAmount(), request.feePercentage());
    }

    private static BigDecimal resolveBuyGrossAmount(BuyTransactionRequest request) {
        if (request.inputMode() == BuyInputMode.USD_AMOUNT) {
            return request.usdAmount().divide(request.unitPriceUsd(), DIVISION_SCALE, RoundingMode.HALF_UP);
        }
        return request.grossAmount();
    }

    private static void validateFeeFields(BigDecimal feeAmount, BigDecimal feePercentage) {
        if (feeAmount != null && feeAmount.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("feeAmount must be zero or positive");
        }
        if (feePercentage != null && feePercentage.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("feePercentage must be zero or positive");
        }
    }

    private static void validateRequest(SellTransactionRequest request) {
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
        validateFeeFields(request.feeAmount(), request.feePercentage());
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

    private static FeeState resolveFeeState(
        BigDecimal grossAmount,
        String assetSymbol,
        BigDecimal feeAmountRequest,
        BigDecimal feePercentageRequest,
        String feeCurrencyRequest
    ) {
        BigDecimal feeAmount = normalizeFee(feeAmountRequest);
        String feeCurrency = normalizeFeeCurrency(feeCurrencyRequest);
        BigDecimal feePercentage = feePercentageRequest;

        if (feePercentage != null) {
            if (feeCurrency == null) {
                feeCurrency = assetSymbol.toUpperCase(Locale.ROOT);
            }
            if (!assetSymbol.equalsIgnoreCase(feeCurrency)) {
                throw new IllegalArgumentException("feePercentage requires asset-denominated feeCurrency");
            }
            feeAmount = grossAmount.multiply(feePercentage);
        } else if (feeAmount.compareTo(ZERO) > 0 && feeCurrency == null) {
            feeCurrency = assetSymbol.toUpperCase(Locale.ROOT);
        }

        if (feePercentage == null && feeAmount.compareTo(ZERO) > 0 && assetSymbol.equalsIgnoreCase(feeCurrency)) {
            feePercentage = feeAmount.divide(grossAmount, FEE_PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        }

        return new FeeState(feeAmount, feePercentage, feeCurrency);
    }

    private static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getUser().getId(),
            transaction.getAsset().getId(),
            transaction.getExchange().getId(),
            transaction.getTransactionType(),
            transaction.getGrossAmount(),
            transaction.getFeeAmount(),
            transaction.getFeePercentage(),
            transaction.getFeeCurrency(),
            transaction.getNetAmount(),
            transaction.getUnitPriceUsd(),
            transaction.getTotalSpentUsd(),
            transaction.getRealizedPnl(),
            transaction.getTransactionDate()
        );
    }

    private PositionState calculateCurrentPosition(UUID userId, UUID assetId) {
        List<Transaction> history = new ArrayList<>(
            transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId)
        );

        history.sort(
            Comparator.comparing(
                Transaction::getTransactionDate,
                Comparator.nullsFirst(Comparator.naturalOrder())
            )
        );

        BigDecimal quantity = ZERO;
        BigDecimal costBasisUsd = ZERO;

        for (Transaction tx : history) {
            if (tx.getTransactionType() == TransactionType.BUY) {
                quantity = quantity.add(tx.getNetAmount());
                costBasisUsd = costBasisUsd.add(tx.getTotalSpentUsd());
                continue;
            }

            if (tx.getTransactionType() == TransactionType.SELL) {
                if (quantity.compareTo(ZERO) <= 0) {
                    throw new IllegalStateException("Invalid position history: sell transaction without balance");
                }
                BigDecimal avgCost = costBasisUsd.divide(quantity, 18, java.math.RoundingMode.HALF_UP);
                BigDecimal soldQty = tx.getGrossAmount();
                BigDecimal costReduction = avgCost.multiply(soldQty);
                quantity = quantity.subtract(soldQty);
                costBasisUsd = costBasisUsd.subtract(costReduction);
            }
        }

        return new PositionState(quantity, costBasisUsd);
    }

    private record PositionState(BigDecimal quantity, BigDecimal costBasisUsd) {
    }

    private record FeeState(BigDecimal feeAmount, BigDecimal feePercentage, String feeCurrency) {
    }
}
