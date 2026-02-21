package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.BuyInputMode;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.enums.TransactionAccumulationRole;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.dto.transaction.UpdateTransactionNetAmountRequest;
import com.trading.dto.transaction.UpdateTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final String USD = "USD";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int DIVISION_SCALE = 18;
    private static final int FEE_PERCENTAGE_SCALE = 6;
    private static final int COST_SCALE = 18;

    private final TransactionRepository transactionRepository;
    private final AccumulationTradeRepository accumulationTradeRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final ExchangeRepository exchangeRepository;
    private final PricePeakRepository pricePeakRepository;

    public TransactionServiceImpl(
        TransactionRepository transactionRepository,
        AccumulationTradeRepository accumulationTradeRepository,
        UserRepository userRepository,
        AssetRepository assetRepository,
        ExchangeRepository exchangeRepository,
        PricePeakRepository pricePeakRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accumulationTradeRepository = accumulationTradeRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.exchangeRepository = exchangeRepository;
        this.pricePeakRepository = pricePeakRepository;
    }

    @Override
    public Page<TransactionResponse> list(UUID userId, int page, int size, String search) {
        Objects.requireNonNull(userId, "userId is required");
        String searchPattern = toSearchPattern(search);
        Sort sort = Sort.by(
            Sort.Order.asc("exchange.name"),
            Sort.Order.asc("asset.symbol"),
            Sort.Order.desc("transactionDate")
        );
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Transaction> transactionPage = transactionRepository.findByUser_IdAndSearch(userId, searchPattern, pageRequest);
        List<Transaction> allUserTransactions = transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId);
        Map<UUID, UUID> matchedPairs = buildMatchedPairsByTransactionId(allUserTransactions);
        Map<UUID, TransactionAccumulationRole> accumulationRoles = buildAccumulationRolesByTransactionId(userId);
        List<TransactionResponse> content = transactionPage.getContent().stream()
            .map((transaction) -> toResponse(
                transaction,
                matchedPairs.get(transaction.getId()),
                accumulationRoles.getOrDefault(transaction.getId(), TransactionAccumulationRole.NONE)
            ))
            .toList();
        return new PageImpl<>(content, pageRequest, transactionPage.getTotalElements());
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
            request.unitPriceUsd(),
            asset.getSymbol(),
            request.feeAmount(),
            request.feePercentage(),
            request.feeCurrency(),
            false
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
                .orElse(saved),
            null,
            TransactionAccumulationRole.NONE
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
            request.unitPriceUsd(),
            asset.getSymbol(),
            request.feeAmount(),
            request.feePercentage(),
            request.feeCurrency(),
            true
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
                .orElse(saved),
            null,
            TransactionAccumulationRole.NONE
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
        return toResponse(refreshed, null, TransactionAccumulationRole.NONE);
    }

    @Override
    public TransactionResponse updateTransactionNetAmount(
        UUID userId,
        UUID transactionId,
        UpdateTransactionNetAmountRequest request
    ) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(transactionId, "transactionId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.netAmount(), "netAmount is required");
        if (request.netAmount().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("netAmount must be positive");
        }

        Transaction tx = transactionRepository.findByIdAndUser_Id(transactionId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        tx.setNetAmount(request.netAmount());
        Transaction saved = transactionRepository.save(tx);
        return toResponse(saved, null, TransactionAccumulationRole.NONE);
    }

    @Override
    @Transactional
    public void deleteTransaction(UUID userId, UUID transactionId) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(transactionId, "transactionId is required");

        Transaction tx = transactionRepository.findByIdAndUser_Id(transactionId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        UUID assetId = tx.getAsset().getId();

        removeAccumulationTradesReferencingTransaction(userId, transactionId);
        clearDeletedBuyReferenceFromPricePeak(userId, assetId, tx);
        transactionRepository.delete(tx);
        recalculateAssetTransactions(userId, assetId);
    }

    private void removeAccumulationTradesReferencingTransaction(UUID userId, UUID transactionId) {
        List<AccumulationTrade> linkedAsReentry =
            accumulationTradeRepository.findAllByUser_IdAndReentryTransaction_Id(userId, transactionId);
        List<AccumulationTrade> linkedAsExit =
            accumulationTradeRepository.findAllByUser_IdAndExitTransaction_Id(userId, transactionId);

        if (!linkedAsReentry.isEmpty()) {
            accumulationTradeRepository.deleteAll(linkedAsReentry);
        }
        if (!linkedAsExit.isEmpty()) {
            accumulationTradeRepository.deleteAll(linkedAsExit);
        }
    }

    private void clearDeletedBuyReferenceFromPricePeak(UUID userId, UUID assetId, Transaction deletedTransaction) {
        if (deletedTransaction.getTransactionType() != TransactionType.BUY) {
            return;
        }

        Optional<PricePeak> pricePeakOptional = pricePeakRepository.findByUser_IdAndAsset_Id(userId, assetId);
        if (pricePeakOptional.isEmpty()) {
            return;
        }

        PricePeak pricePeak = pricePeakOptional.get();
        Transaction lastBuyTransaction = pricePeak.getLastBuyTransaction();
        if (lastBuyTransaction == null || !deletedTransaction.getId().equals(lastBuyTransaction.getId())) {
            return;
        }

        Optional<Transaction> replacementBuy = transactionRepository
            .findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(userId, assetId, TransactionType.BUY)
            .stream()
            .filter(existingBuy -> !deletedTransaction.getId().equals(existingBuy.getId()))
            .findFirst();

        if (replacementBuy.isPresent()) {
            Transaction replacement = replacementBuy.get();
            pricePeak.setLastBuyTransaction(replacement);
            pricePeak.setPeakPrice(replacement.getUnitPriceUsd());
            pricePeak.setPeakTimestamp(replacement.getTransactionDate());
            pricePeak.setActive(true);
        } else {
            pricePeak.setLastBuyTransaction(null);
            pricePeak.setActive(false);
        }

        pricePeakRepository.save(pricePeak);
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

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private static String toSearchPattern(String search) {
        String normalizedSearch = normalizeSearch(search);
        if (normalizedSearch == null) {
            return null;
        }
        return "%" + normalizedSearch + "%";
    }

    private static FeeState resolveFeeState(
        BigDecimal grossAmount,
        BigDecimal unitPriceUsd,
        String assetSymbol,
        BigDecimal feeAmountRequest,
        BigDecimal feePercentageRequest,
        String feeCurrencyRequest,
        boolean isSell
    ) {
        BigDecimal feeAmount = normalizeFee(feeAmountRequest);
        String feeCurrency = normalizeFeeCurrency(feeCurrencyRequest);
        BigDecimal feePercentage = feePercentageRequest;

        if (feePercentage != null) {
            if (isSell) {
                if (feeCurrency == null) {
                    feeCurrency = USD;
                }
                if (!USD.equalsIgnoreCase(feeCurrency)) {
                    throw new IllegalArgumentException("sell feePercentage requires USD feeCurrency");
                }
                feeAmount = grossAmount.multiply(unitPriceUsd).multiply(feePercentage);
            } else {
                if (feeCurrency == null) {
                    feeCurrency = assetSymbol.toUpperCase(Locale.ROOT);
                }
                if (!assetSymbol.equalsIgnoreCase(feeCurrency)) {
                    throw new IllegalArgumentException("buy feePercentage requires asset-denominated feeCurrency");
                }
                feeAmount = grossAmount.multiply(feePercentage);
            }
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
                tx.getUnitPriceUsd(),
                tx.getAsset().getSymbol(),
                tx.getFeeAmount(),
                tx.getFeePercentage(),
                tx.getFeeCurrency(),
                tx.getTransactionType() == TransactionType.SELL
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

                BigDecimal soldCostBasisUsd;
                if (runningQuantity.compareTo(tx.getGrossAmount()) == 0) {
                    soldCostBasisUsd = runningCostBasisUsd;
                    runningCostBasisUsd = ZERO;
                } else {
                    BigDecimal averageCostPerUnit = runningQuantity.compareTo(ZERO) == 0
                        ? ZERO
                        : runningCostBasisUsd.divide(runningQuantity, COST_SCALE, RoundingMode.HALF_UP);
                    soldCostBasisUsd = averageCostPerUnit.multiply(tx.getGrossAmount());
                    runningCostBasisUsd = runningCostBasisUsd.subtract(soldCostBasisUsd);
                }
                tx.setRealizedPnl(tx.getTotalSpentUsd().subtract(soldCostBasisUsd));

                runningQuantity = runningQuantity.subtract(tx.getGrossAmount());
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

    private static Map<UUID, UUID> buildMatchedPairsByTransactionId(List<Transaction> transactions) {
        List<Transaction> ordered = new ArrayList<>(transactions);
        ordered.sort(
            Comparator.comparing(
                Transaction::getTransactionDate,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ).thenComparing(Transaction::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
        );

        Map<AssetAmountKey, Deque<Transaction>> unmatchedBuysByAssetAmount = new HashMap<>();
        Map<UUID, UUID> matchedPairsByTransactionId = new HashMap<>();

        for (Transaction tx : ordered) {
            if (tx.getTransactionType() == TransactionType.BUY) {
                AssetAmountKey key = new AssetAmountKey(tx.getAsset().getId(), normalizeAmountKey(tx.getNetAmount()));
                unmatchedBuysByAssetAmount.computeIfAbsent(key, ignored -> new LinkedList<>()).addLast(tx);
                continue;
            }

            if (tx.getTransactionType() == TransactionType.SELL) {
                AssetAmountKey key = new AssetAmountKey(tx.getAsset().getId(), normalizeAmountKey(tx.getGrossAmount()));
                Deque<Transaction> candidates = unmatchedBuysByAssetAmount.get(key);
                if (candidates == null || candidates.isEmpty()) {
                    continue;
                }

                Transaction matchedBuy = candidates.pollFirst();
                if (matchedBuy == null) {
                    continue;
                }

                matchedPairsByTransactionId.put(matchedBuy.getId(), tx.getId());
                matchedPairsByTransactionId.put(tx.getId(), matchedBuy.getId());
            }
        }

        return matchedPairsByTransactionId;
    }

    private static String normalizeAmountKey(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private Map<UUID, TransactionAccumulationRole> buildAccumulationRolesByTransactionId(UUID userId) {
        List<AccumulationTrade> accumulationTrades = accumulationTradeRepository.findAllByUser_IdOrderByCreatedAtDesc(userId);
        Map<UUID, TransactionAccumulationRole> roles = new HashMap<>();
        for (AccumulationTrade trade : accumulationTrades) {
            if (trade.getExitTransaction() != null && trade.getExitTransaction().getId() != null) {
                roles.put(trade.getExitTransaction().getId(), TransactionAccumulationRole.ACCUMULATION_EXIT);
            }
            if (trade.getReentryTransaction() != null && trade.getReentryTransaction().getId() != null) {
                roles.put(trade.getReentryTransaction().getId(), TransactionAccumulationRole.ACCUMULATION_REENTRY);
            }
        }
        return roles;
    }

    private static TransactionResponse toResponse(
        Transaction transaction,
        UUID matchedTransactionId,
        TransactionAccumulationRole accumulationRole
    ) {
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
            transaction.getTransactionDate(),
            matchedTransactionId != null,
            matchedTransactionId,
            accumulationRole != TransactionAccumulationRole.NONE,
            accumulationRole
        );
    }

    private record AssetAmountKey(UUID assetId, String amount) {
    }

    private record FeeState(BigDecimal feeAmount, BigDecimal feePercentage, String feeCurrency) {
    }

    private record AmountState(BigDecimal netAmount, BigDecimal totalUsd) {
    }
}
