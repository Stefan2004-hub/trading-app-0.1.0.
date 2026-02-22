package com.trading.service.transaction;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Exchange;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.BuyInputMode;
import com.trading.domain.enums.TransactionListView;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.enums.TransactionAccumulationRole;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.CleanHistoryBackup;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.dto.transaction.UpdateTransactionNetAmountRequest;
import com.trading.dto.transaction.UpdateTransactionRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final String USD = "USD";
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int DIVISION_SCALE = 18;
    private static final int FEE_PERCENTAGE_SCALE = 6;
    private static final int COST_SCALE = 18;
    private static final BigDecimal MATCH_EPSILON = new BigDecimal("0.000000000001");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Comparator<MatchedPairGroup> MATCHED_PAIR_GROUP_COMPARATOR = (left, right) -> {
        OffsetDateTime leftDate = left.latestDate();
        OffsetDateTime rightDate = right.latestDate();
        if (leftDate == null && rightDate != null) {
            return 1;
        }
        if (leftDate != null && rightDate == null) {
            return -1;
        }
        if (leftDate != null) {
            int byDateDesc = rightDate.compareTo(leftDate);
            if (byDateDesc != 0) {
                return byDateDesc;
            }
        }
        int byBuyId = left.buy().getId().compareTo(right.buy().getId());
        if (byBuyId != 0) {
            return byBuyId;
        }
        return left.sell().getId().compareTo(right.sell().getId());
    };

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
    public Page<TransactionResponse> list(
        UUID userId,
        int page,
        int size,
        String search,
        TransactionListView view,
        int groupSize
    ) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(view, "view is required");
        if (groupSize <= 0) {
            throw new IllegalArgumentException("groupSize must be positive");
        }
        if (view == TransactionListView.MATCHED) {
            return listMatched(userId, page, groupSize, search);
        }

        return listOpen(userId, page, size, search);
    }

    private Page<TransactionResponse> listOpen(UUID userId, int page, int size, String search) {
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

    private Page<TransactionResponse> listMatched(UUID userId, int page, int groupSize, String search) {
        int matchedRowPageSize = groupSize * 2;
        List<Transaction> allUserTransactions = transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId);
        if (allUserTransactions.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, matchedRowPageSize), 0);
        }

        Map<UUID, UUID> matchedPairs = buildMatchedPairsByTransactionId(allUserTransactions);
        Map<UUID, TransactionAccumulationRole> accumulationRoles = buildAccumulationRolesByTransactionId(userId);
        List<MatchedPairGroup> allGroups = buildMatchedPairGroups(allUserTransactions, search);

        int totalGroups = allGroups.size();
        int fromIndex = page * groupSize;
        if (fromIndex >= totalGroups) {
            return new PageImpl<>(List.of(), PageRequest.of(page, matchedRowPageSize), totalGroups * 2L);
        }

        int toIndex = Math.min(fromIndex + groupSize, totalGroups);
        List<MatchedPairGroup> pageGroups = allGroups.subList(fromIndex, toIndex);
        List<TransactionResponse> content = new ArrayList<>(pageGroups.size() * 2);
        for (MatchedPairGroup group : pageGroups) {
            content.add(
                toResponse(
                    group.buy(),
                    group.sell().getId(),
                    accumulationRoles.getOrDefault(group.buy().getId(), TransactionAccumulationRole.NONE)
                )
            );
            content.add(
                toResponse(
                    group.sell(),
                    group.buy().getId(),
                    accumulationRoles.getOrDefault(group.sell().getId(), TransactionAccumulationRole.NONE)
                )
            );
        }

        return new PageImpl<>(content, PageRequest.of(page, matchedRowPageSize), totalGroups * 2L);
    }

    @Override
    @CacheEvict(cacheNames = "portfolioAssetSummary", key = "#userId")
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
    @CacheEvict(cacheNames = "portfolioAssetSummary", key = "#userId")
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
    @CacheEvict(cacheNames = "portfolioAssetSummary", key = "#userId")
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
    @CacheEvict(cacheNames = "portfolioAssetSummary", key = "#userId")
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
    @CacheEvict(cacheNames = "portfolioAssetSummary", key = "#userId")
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

    @Override
    @Transactional
    @CacheEvict(cacheNames = "portfolioAssetSummary", key = "#userId")
    public CleanHistoryBackup cleanHistory(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");

        List<Transaction> allUserTransactions = transactionRepository.findAllByUser_IdOrderByTransactionDateDesc(userId);
        if (allUserTransactions.isEmpty()) {
            byte[] fileContent = createCleanHistoryWorkbook(List.of(), List.of());
            return new CleanHistoryBackup(fileContent, cleanHistoryFileName());
        }

        List<MatchedPairGroup> matchedGroups = buildMatchedPairGroups(allUserTransactions, null);
        if (matchedGroups.isEmpty()) {
            byte[] fileContent = createCleanHistoryWorkbook(List.of(), List.of());
            return new CleanHistoryBackup(fileContent, cleanHistoryFileName());
        }

        Set<UUID> transactionIdsToDelete = new LinkedHashSet<>();
        List<Transaction> matchedTransactions = new ArrayList<>(matchedGroups.size() * 2);
        for (MatchedPairGroup group : matchedGroups) {
            transactionIdsToDelete.add(group.buy().getId());
            transactionIdsToDelete.add(group.sell().getId());
            matchedTransactions.add(group.buy());
            matchedTransactions.add(group.sell());
        }

        List<AccumulationTrade> linkedAccumulationTrades =
            accumulationTradeRepository.findAllLinkedToTransactions(userId, transactionIdsToDelete);

        byte[] fileContent = createCleanHistoryWorkbook(matchedTransactions, linkedAccumulationTrades);

        clearDeletedBuyReferencesFromPricePeaks(userId, transactionIdsToDelete);

        if (!linkedAccumulationTrades.isEmpty()) {
            accumulationTradeRepository.deleteAll(linkedAccumulationTrades);
        }

        List<Transaction> persistedTransactions =
            transactionRepository.findAllByUser_IdAndIdIn(userId, transactionIdsToDelete);
        if (!persistedTransactions.isEmpty()) {
            transactionRepository.deleteAll(persistedTransactions);
        }

        recalculateAssetsAfterBulkDelete(userId, matchedTransactions);
        return new CleanHistoryBackup(fileContent, cleanHistoryFileName());
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

    private void recalculateAssetsAfterBulkDelete(UUID userId, Collection<Transaction> matchedTransactions) {
        Set<UUID> affectedAssetIds = new LinkedHashSet<>();
        for (Transaction tx : matchedTransactions) {
            if (tx.getAsset() != null && tx.getAsset().getId() != null) {
                affectedAssetIds.add(tx.getAsset().getId());
            }
        }
        for (UUID assetId : affectedAssetIds) {
            recalculateAssetTransactions(userId, assetId);
        }
    }

    private void clearDeletedBuyReferencesFromPricePeaks(UUID userId, Collection<UUID> transactionIdsToDelete) {
        if (transactionIdsToDelete.isEmpty()) {
            return;
        }
        List<PricePeak> affectedPeaks = pricePeakRepository.findAllByUser_IdAndLastBuyTransaction_IdIn(
            userId,
            transactionIdsToDelete
        );
        if (affectedPeaks.isEmpty()) {
            return;
        }

        for (PricePeak pricePeak : affectedPeaks) {
            UUID assetId = pricePeak.getAsset() == null ? null : pricePeak.getAsset().getId();
            if (assetId == null) {
                continue;
            }
            Optional<Transaction> replacementBuy = transactionRepository
                .findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(userId, assetId, TransactionType.BUY)
                .stream()
                .filter(existingBuy -> !transactionIdsToDelete.contains(existingBuy.getId()))
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
        }
        pricePeakRepository.saveAll(affectedPeaks);
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

    private static boolean matchesSearch(Transaction tx, String normalizedSearch) {
        String lowerSearch = normalizedSearch.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(tx.getAsset().getSymbol(), lowerSearch)
            || containsIgnoreCase(tx.getAsset().getName(), lowerSearch)
            || containsIgnoreCase(tx.getExchange().getSymbol(), lowerSearch)
            || containsIgnoreCase(tx.getExchange().getName(), lowerSearch);
    }

    private static boolean containsIgnoreCase(String value, String normalizedSearch) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedSearch);
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

    private static List<MatchedPairGroup> buildMatchedPairGroups(List<Transaction> allUserTransactions, String search) {
        Map<UUID, UUID> matchedPairs = buildMatchedPairsByTransactionId(allUserTransactions);
        Map<UUID, Transaction> transactionsById = new HashMap<>();
        for (Transaction transaction : allUserTransactions) {
            transactionsById.put(transaction.getId(), transaction);
        }

        String normalizedSearch = normalizeSearch(search);
        Set<UUID> visited = new java.util.HashSet<>();
        List<MatchedPairGroup> groups = new ArrayList<>();

        for (Transaction transaction : allUserTransactions) {
            UUID transactionId = transaction.getId();
            UUID matchedId = matchedPairs.get(transactionId);
            if (matchedId == null || visited.contains(transactionId)) {
                continue;
            }

            Transaction matchedTransaction = transactionsById.get(matchedId);
            if (matchedTransaction == null || visited.contains(matchedId)) {
                continue;
            }

            Transaction buy = transaction.getTransactionType() == TransactionType.BUY ? transaction : matchedTransaction;
            Transaction sell = transaction.getTransactionType() == TransactionType.SELL ? transaction : matchedTransaction;
            if (buy.getTransactionType() != TransactionType.BUY || sell.getTransactionType() != TransactionType.SELL) {
                continue;
            }

            if (normalizedSearch != null
                && !matchesSearch(buy, normalizedSearch)
                && !matchesSearch(sell, normalizedSearch)) {
                continue;
            }

            visited.add(transactionId);
            visited.add(matchedId);
            groups.add(new MatchedPairGroup(buy, sell));
        }

        groups.sort(MATCHED_PAIR_GROUP_COMPARATOR);
        return groups;
    }

    protected byte[] createCleanHistoryWorkbook(
        List<Transaction> matchedTransactions,
        List<AccumulationTrade> linkedAccumulationTrades
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            createSummarySheet(workbook, matchedTransactions, linkedAccumulationTrades);
            createMatchedTransactionsSheet(workbook, matchedTransactions);
            createAccumulationTradesSheet(workbook, linkedAccumulationTrades);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate clean history backup workbook", exception);
        }
    }

    private static void createSummarySheet(
        Workbook workbook,
        List<Transaction> matchedTransactions,
        List<AccumulationTrade> linkedAccumulationTrades
    ) {
        BigDecimal totalInvested = matchedTransactions.stream()
            .filter(tx -> tx.getTransactionType() == TransactionType.BUY)
            .map(Transaction::getTotalSpentUsd)
            .filter(Objects::nonNull)
            .reduce(ZERO, BigDecimal::add);
        BigDecimal totalRealizedPnl = matchedTransactions.stream()
            .filter(tx -> tx.getTransactionType() == TransactionType.SELL)
            .map(Transaction::getRealizedPnl)
            .map(value -> value == null ? ZERO : value)
            .reduce(ZERO, BigDecimal::add);

        Sheet sheet = workbook.createSheet("Summary Report");
        int rowIndex = 0;
        Row header = sheet.createRow(rowIndex++);
        header.createCell(0).setCellValue("Metric");
        header.createCell(1).setCellValue("Value");

        Row investedRow = sheet.createRow(rowIndex++);
        investedRow.createCell(0).setCellValue("Total Invested");
        investedRow.createCell(1).setCellValue(totalInvested.toPlainString());

        Row pnlRow = sheet.createRow(rowIndex++);
        pnlRow.createCell(0).setCellValue("Total Realized PnL");
        pnlRow.createCell(1).setCellValue(totalRealizedPnl.toPlainString());

        Row matchedCountRow = sheet.createRow(rowIndex++);
        matchedCountRow.createCell(0).setCellValue("Deleted Transactions");
        matchedCountRow.createCell(1).setCellValue(matchedTransactions.size());

        Row accumulationCountRow = sheet.createRow(rowIndex);
        accumulationCountRow.createCell(0).setCellValue("Deleted Accumulation Trades");
        accumulationCountRow.createCell(1).setCellValue(linkedAccumulationTrades.size());

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private static void createMatchedTransactionsSheet(Workbook workbook, List<Transaction> matchedTransactions) {
        Sheet sheet = workbook.createSheet("Matched Transactions");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Transaction ID");
        header.createCell(1).setCellValue("Type");
        header.createCell(2).setCellValue("Asset");
        header.createCell(3).setCellValue("Exchange");
        header.createCell(4).setCellValue("Gross Amount");
        header.createCell(5).setCellValue("Net Amount");
        header.createCell(6).setCellValue("Unit Price USD");
        header.createCell(7).setCellValue("Total USD");
        header.createCell(8).setCellValue("Realized PnL");
        header.createCell(9).setCellValue("Date");

        int rowIndex = 1;
        for (Transaction tx : matchedTransactions) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(safe(tx.getId()));
            row.createCell(1).setCellValue(tx.getTransactionType().name());
            row.createCell(2).setCellValue(tx.getAsset() == null ? "" : safe(tx.getAsset().getSymbol()));
            row.createCell(3).setCellValue(tx.getExchange() == null ? "" : safe(tx.getExchange().getName()));
            row.createCell(4).setCellValue(toStringValue(tx.getGrossAmount()));
            row.createCell(5).setCellValue(toStringValue(tx.getNetAmount()));
            row.createCell(6).setCellValue(toStringValue(tx.getUnitPriceUsd()));
            row.createCell(7).setCellValue(toStringValue(tx.getTotalSpentUsd()));
            row.createCell(8).setCellValue(toStringValue(tx.getRealizedPnl()));
            row.createCell(9).setCellValue(tx.getTransactionDate() == null ? "" : tx.getTransactionDate().toString());
        }

        for (int i = 0; i <= 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void createAccumulationTradesSheet(Workbook workbook, List<AccumulationTrade> linkedAccumulationTrades) {
        Sheet sheet = workbook.createSheet("Accumulation Trades");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Trade ID");
        header.createCell(1).setCellValue("Status");
        header.createCell(2).setCellValue("Asset");
        header.createCell(3).setCellValue("Exit Transaction ID");
        header.createCell(4).setCellValue("Reentry Transaction ID");
        header.createCell(5).setCellValue("Old Coin Amount");
        header.createCell(6).setCellValue("New Coin Amount");
        header.createCell(7).setCellValue("Accumulation Delta");
        header.createCell(8).setCellValue("Exit Price USD");
        header.createCell(9).setCellValue("Reentry Price USD");
        header.createCell(10).setCellValue("Created At");
        header.createCell(11).setCellValue("Closed At");

        int rowIndex = 1;
        for (AccumulationTrade trade : linkedAccumulationTrades) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(safe(trade.getId()));
            row.createCell(1).setCellValue(trade.getStatus() == null ? "" : trade.getStatus().name());
            row.createCell(2).setCellValue(trade.getAsset() == null ? "" : safe(trade.getAsset().getSymbol()));
            row.createCell(3).setCellValue(
                trade.getExitTransaction() == null ? "" : safe(trade.getExitTransaction().getId())
            );
            row.createCell(4).setCellValue(
                trade.getReentryTransaction() == null ? "" : safe(trade.getReentryTransaction().getId())
            );
            row.createCell(5).setCellValue(toStringValue(trade.getOldCoinAmount()));
            row.createCell(6).setCellValue(toStringValue(trade.getNewCoinAmount()));
            row.createCell(7).setCellValue(toStringValue(trade.getAccumulationDelta()));
            row.createCell(8).setCellValue(toStringValue(trade.getExitPriceUsd()));
            row.createCell(9).setCellValue(toStringValue(trade.getReentryPriceUsd()));
            row.createCell(10).setCellValue(trade.getCreatedAt() == null ? "" : trade.getCreatedAt().toString());
            row.createCell(11).setCellValue(trade.getClosedAt() == null ? "" : trade.getClosedAt().toString());
        }

        for (int i = 0; i <= 11; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static String toStringValue(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String safe(UUID value) {
        return value == null ? "" : value.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String cleanHistoryFileName() {
        String timestamp = FILE_TIMESTAMP_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC));
        return "trading-history-backup-" + timestamp + ".xlsx";
    }

    private static Map<UUID, UUID> buildMatchedPairsByTransactionId(List<Transaction> transactions) {
        List<Transaction> ordered = new ArrayList<>(transactions);
        ordered.sort(
            Comparator.comparing(
                Transaction::getTransactionDate,
                Comparator.nullsFirst(Comparator.naturalOrder())
            ).thenComparing(Transaction::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
        );

        Map<AssetExchangeKey, List<Transaction>> unmatchedBuysByAssetExchange = new HashMap<>();
        Map<UUID, UUID> matchedPairsByTransactionId = new HashMap<>();

        for (Transaction tx : ordered) {
            if (tx.getTransactionType() == TransactionType.BUY) {
                AssetExchangeKey key = new AssetExchangeKey(tx.getAsset().getId(), tx.getExchange().getId());
                unmatchedBuysByAssetExchange.computeIfAbsent(key, ignored -> new LinkedList<>()).add(tx);
                continue;
            }

            if (tx.getTransactionType() == TransactionType.SELL) {
                AssetExchangeKey key = new AssetExchangeKey(tx.getAsset().getId(), tx.getExchange().getId());
                List<Transaction> candidates = unmatchedBuysByAssetExchange.get(key);
                if (candidates == null || candidates.isEmpty()) {
                    continue;
                }

                int bestIndex = -1;
                BigDecimal bestDifference = null;
                for (int i = 0; i < candidates.size(); i++) {
                    Transaction candidate = candidates.get(i);
                    BigDecimal difference = candidate.getNetAmount().subtract(tx.getGrossAmount()).abs();
                    if (difference.compareTo(MATCH_EPSILON) > 0) {
                        continue;
                    }
                    if (bestDifference == null || difference.compareTo(bestDifference) < 0) {
                        bestDifference = difference;
                        bestIndex = i;
                        continue;
                    }
                    if (difference.compareTo(bestDifference) == 0 && isOlderThan(candidate, candidates.get(bestIndex))) {
                        bestIndex = i;
                    }
                }

                if (bestIndex < 0) {
                    continue;
                }
                Transaction matchedBuy = candidates.remove(bestIndex);

                matchedPairsByTransactionId.put(matchedBuy.getId(), tx.getId());
                matchedPairsByTransactionId.put(tx.getId(), matchedBuy.getId());
            }
        }

        return matchedPairsByTransactionId;
    }

    private static boolean isOlderThan(Transaction left, Transaction right) {
        OffsetDateTime leftDate = left.getTransactionDate();
        OffsetDateTime rightDate = right.getTransactionDate();

        if (leftDate == null && rightDate != null) {
            return true;
        }
        if (leftDate != null && rightDate == null) {
            return false;
        }
        if (leftDate != null && rightDate != null) {
            int dateCompare = leftDate.compareTo(rightDate);
            if (dateCompare != 0) {
                return dateCompare < 0;
            }
        }

        UUID leftId = left.getId();
        UUID rightId = right.getId();
        if (leftId == null && rightId != null) {
            return true;
        }
        if (leftId != null && rightId == null) {
            return false;
        }
        if (leftId == null && rightId == null) {
            return false;
        }
        return leftId.compareTo(rightId) < 0;
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

    private record AssetExchangeKey(UUID assetId, UUID exchangeId) {
    }

    private record FeeState(BigDecimal feeAmount, BigDecimal feePercentage, String feeCurrency) {
    }

    private record AmountState(BigDecimal netAmount, BigDecimal totalUsd) {
    }

    private record MatchedPairGroup(Transaction buy, Transaction sell) {
        private OffsetDateTime latestDate() {
            OffsetDateTime buyDate = buy.getTransactionDate();
            OffsetDateTime sellDate = sell.getTransactionDate();
            if (buyDate == null) {
                return sellDate;
            }
            if (sellDate == null) {
                return buyDate;
            }
            return buyDate.isAfter(sellDate) ? buyDate : sellDate;
        }
    }
}
