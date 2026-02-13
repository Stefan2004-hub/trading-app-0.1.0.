package com.trading.domain.entity;

import com.trading.domain.enums.AccumulationTradeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accumulation_trades")
public class AccumulationTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_transaction_id", nullable = false)
    private Transaction exitTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reentry_transaction_id")
    private Transaction reentryTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "old_coin_amount", nullable = false, precision = 20, scale = 18)
    private BigDecimal oldCoinAmount;

    @Column(name = "new_coin_amount", precision = 20, scale = 18)
    private BigDecimal newCoinAmount;

    @Column(name = "accumulation_delta", precision = 20, scale = 18, insertable = false, updatable = false)
    private BigDecimal accumulationDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AccumulationTradeStatus status;

    @Column(name = "exit_price_usd", nullable = false, precision = 20, scale = 18)
    private BigDecimal exitPriceUsd;

    @Column(name = "reentry_price_usd", precision = 20, scale = 18)
    private BigDecimal reentryPriceUsd;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "prediction_notes")
    private String predictionNotes;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Transaction getExitTransaction() {
        return exitTransaction;
    }

    public void setExitTransaction(Transaction exitTransaction) {
        this.exitTransaction = exitTransaction;
    }

    public Transaction getReentryTransaction() {
        return reentryTransaction;
    }

    public void setReentryTransaction(Transaction reentryTransaction) {
        this.reentryTransaction = reentryTransaction;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public BigDecimal getOldCoinAmount() {
        return oldCoinAmount;
    }

    public void setOldCoinAmount(BigDecimal oldCoinAmount) {
        this.oldCoinAmount = oldCoinAmount;
    }

    public BigDecimal getNewCoinAmount() {
        return newCoinAmount;
    }

    public void setNewCoinAmount(BigDecimal newCoinAmount) {
        this.newCoinAmount = newCoinAmount;
    }

    public BigDecimal getAccumulationDelta() {
        return accumulationDelta;
    }

    public void setAccumulationDelta(BigDecimal accumulationDelta) {
        this.accumulationDelta = accumulationDelta;
    }

    public AccumulationTradeStatus getStatus() {
        return status;
    }

    public void setStatus(AccumulationTradeStatus status) {
        this.status = status;
    }

    public BigDecimal getExitPriceUsd() {
        return exitPriceUsd;
    }

    public void setExitPriceUsd(BigDecimal exitPriceUsd) {
        this.exitPriceUsd = exitPriceUsd;
    }

    public BigDecimal getReentryPriceUsd() {
        return reentryPriceUsd;
    }

    public void setReentryPriceUsd(BigDecimal reentryPriceUsd) {
        this.reentryPriceUsd = reentryPriceUsd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getPredictionNotes() {
        return predictionNotes;
    }

    public void setPredictionNotes(String predictionNotes) {
        this.predictionNotes = predictionNotes;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
