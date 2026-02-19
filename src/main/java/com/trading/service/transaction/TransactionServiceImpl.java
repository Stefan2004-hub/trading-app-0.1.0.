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
import com.trading.dto.transaction.UpdateTransactionRequest;
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
    private static final int COST_SCALE = 18;

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

        AmountState amountState = calculateBuyAmounts(grossAmount, request.unitPriceUsd(), feeState, asset.getSymbol());

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAsset(asset);
        tx.setExchange(exchange);
        tx.setTransactionType(TransactionType.BUY);
        tx.setGrossAmount(grossAmount);
        tx.setFeeAmount(feeState.feeAmount());
        tx.setFeePercentage(feeState.feePercentage());
        tx.setFeeCurrency(feeState.feeCurrency());
        tx.setNetAmount(amountState.netAmount());
        tx.setUnitPriceUsd(request.unitPriceUsd());
        tx.setTotalSpentUsd(amountState.totalUsd());
        tx.setRealizedPnl(null);
        tx.setTransactionDate(request.transactionDate() == null ? OffsetDateTime.now() : request.transactionDate());

        Transaction saved = transactionRepository.save(tx);
        recalculateAssetTransactions(userId, asset.getId());
        return toResponse(
            transactionRepository.findByIdAndUser_Id(saved.getId(), userId)
                .orElse(saved)
        );
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

        AmountState amountState = calculateSellAmounts(request.grossAmount(), request.unitPriceUsd(), feeState, asset.getSymbol());

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAsset(asset);
        tx.setExchange(exchange);
        tx.setTransactionType(TransactionType.SELL);
        tx.setGrossAmount(request.grossAmount());
        tx.setFeeAmount(feeState.feeAmount());
        tx.setFeePercentage(feeState.feePercentage());
        tx.setFeeCurrency(feeState.feeCurrency());
        tx.setNetAmount(amountState.netAmount());
        tx.setUnitPriceUsd(request.unitPriceUsd());
        tx.setTotalSpentUsd(amountState.totalUsd());
        tx.setRealizedPnl(null);
        tx.setTransactionDate(request.transactionDate() == null ? OffsetDateTime.now() : request.transactionDate());

        Transaction saved = transactionRepository.save(tx);
        recalculateAssetTransactions(userId, asset.getId());
        return toResponse(
            transactionRepository.findByIdAndUser_Id(saved.getId(), userId)
                .orElse(saved)
        );
    }

    @Override
    public TransactionResponse updateTransaction(UUID userId, UUID transactionId, UpdateTransactionRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(transactionId, "transactionId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.grossAmount(), "grossAmount is required");
        Objects.requireNonNull(request.unitPriceUsd(), "unitPriceUsd is required");

        Transaction tx = transactionRepository.findByIdAndUser_Id(transactionId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        tx.setGrossAmount(request.grossAmount());
        tx.setFeeAmount(request.feeAmount());
        tx.setFeePercentage(request.feePercentage());
        tx.setUnitPriceUsd(request.unitPriceUsd());

        transactionRepository.save(tx);
        recalculateAssetTransactions(userId, tx.getAsset().getId());

        Transaction refreshed = transactionRepository.findByIdAndUser_Id(transactionId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        return toResponse(refreshed);
    }

    @Override
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(transactionId, "transactionId is required");

        Transaction tx = transactionRepository.findByIdAndUser_Id(transactionId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        UUID assetId = tx.getAsset().getId();

        transactionRepository.delete(tx);
        recalculateAssetTransactions(userId, assetId);
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
            if (grossAmount.compareTo(ZERO) == 0) {
                feePercentage = ZERO;
            } else {
                feePercentage = feeAmount.divide(grossAmount, FEE_PERCENTAGE_SCALE, RoundingMode.HALF_UP);
            }
        }

        return new FeeState(feeAmount, feePercentage, feeCurrency);
    }

    private void recalculateAssetTransactions(UUID userId, UUID assetId) {
        List<Transaction> history = new ArrayList<>(
            transactionRepository.findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(userId, assetId)
        );
        if (history.isEmpty()) {
            return;
        }

        history.sort(
            Comparator.comparing(
                Transaction::getTransactionDate,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ).thenComparing(Transaction::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
        );

        BigDecimal runningQuantity = ZERO;
        BigDecimal runningCostBasisUsd = ZERO;

        for (Transaction tx : history) {
            FeeState feeState = resolveFeeState(
                tx.getGrossAmount(),
                tx.getAsset().getSymbol(),
                tx.getFeeAmount(),
                tx.getFeePercentage(),
                tx.getFeeCurrency()
            );
            tx.setFeeAmount(feeState.feeAmount());
            tx.setFeePercentage(feeState.feePercentage());
            tx.setFeeCurrency(feeState.feeCurrency());

            if (tx.getTransactionType() == TransactionType.BUY) {
                AmountState buyAmounts = calculateBuyAmounts(
                    tx.getGrossAmount(),
                    tx.getUnitPriceUsd(),
                    feeState,
                    tx.getAsset().getSymbol()
                );
                tx.setNetAmount(buyAmounts.netAmount());
                tx.setTotalSpentUsd(buyAmounts.totalUsd());
                tx.setRealizedPnl(null);

                runningQuantity = runningQuantity.add(tx.getNetAmount());
                runningCostBasisUsd = runningCostBasisUsd.add(tx.getTotalSpentUsd());
                continue;
            }

            if (tx.getTransactionType() == TransactionType.SELL) {
                AmountState sellAmounts = calculateSellAmounts(
                    tx.getGrossAmount(),
                    tx.getUnitPriceUsd(),
                    feeState,
                    tx.getAsset().getSymbol()
                );
                tx.setNetAmount(sellAmounts.netAmount());
                tx.setTotalSpentUsd(sellAmounts.totalUsd());

                BigDecimal averageCostPerUnit = runningQuantity.compareTo(ZERO) == 0
                    ? ZERO
                    : runningCostBasisUsd.divide(runningQuantity, COST_SCALE, RoundingMode.HALF_UP);
                BigDecimal soldCostBasisUsd = averageCostPerUnit.multiply(tx.getGrossAmount());
                tx.setRealizedPnl(tx.getTotalSpentUsd().subtract(soldCostBasisUsd));

                runningQuantity = runningQuantity.subtract(tx.getGrossAmount());
                runningCostBasisUsd = runningCostBasisUsd.subtract(soldCostBasisUsd);
            }
        }

        transactionRepository.saveAll(history);
    }

    private static AmountState calculateBuyAmounts(
        BigDecimal grossAmount,
        BigDecimal unitPriceUsd,
        FeeState feeState,
        String assetSymbol
    ) {
        BigDecimal totalSpentUsd = grossAmount.multiply(unitPriceUsd);
        BigDecimal netAmount = grossAmount;

        if (feeState.feeAmount().compareTo(ZERO) > 0) {
            if (USD.equals(feeState.feeCurrency())) {
                totalSpentUsd = totalSpentUsd.add(feeState.feeAmount());
            } else if (assetSymbol.equalsIgnoreCase(feeState.feeCurrency())) {
                netAmount = grossAmount.subtract(feeState.feeAmount());
            }
        }

        return new AmountState(netAmount, totalSpentUsd);
    }

    private static AmountState calculateSellAmounts(
        BigDecimal grossAmount,
        BigDecimal unitPriceUsd,
        FeeState feeState,
        String assetSymbol
    ) {
        BigDecimal proceedsUsd = grossAmount.multiply(unitPriceUsd);
        BigDecimal netAmount = grossAmount;

        if (feeState.feeAmount().compareTo(ZERO) > 0) {
            if (USD.equals(feeState.feeCurrency())) {
                proceedsUsd = proceedsUsd.subtract(feeState.feeAmount());
            } else if (assetSymbol.equalsIgnoreCase(feeState.feeCurrency())) {
                netAmount = grossAmount.subtract(feeState.feeAmount());
            }
        }

        return new AmountState(netAmount, proceedsUsd);
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

    private record FeeState(BigDecimal feeAmount, BigDecimal feePercentage, String feeCurrency) {
    }

    private record AmountState(BigDecimal netAmount, BigDecimal totalUsd) {
    }
}
