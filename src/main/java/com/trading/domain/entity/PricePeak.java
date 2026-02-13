package com.trading.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "price_peaks")
public class PricePeak {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_buy_transaction_id")
    private Transaction lastBuyTransaction;

    @Column(name = "peak_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal peakPrice;

    @Column(name = "peak_timestamp", nullable = false)
    private OffsetDateTime peakTimestamp;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Transaction getLastBuyTransaction() {
        return lastBuyTransaction;
    }

    public void setLastBuyTransaction(Transaction lastBuyTransaction) {
        this.lastBuyTransaction = lastBuyTransaction;
    }

    public BigDecimal getPeakPrice() {
        return peakPrice;
    }

    public void setPeakPrice(BigDecimal peakPrice) {
        this.peakPrice = peakPrice;
    }

    public OffsetDateTime getPeakTimestamp() {
        return peakTimestamp;
    }

    public void setPeakTimestamp(OffsetDateTime peakTimestamp) {
        this.peakTimestamp = peakTimestamp;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
