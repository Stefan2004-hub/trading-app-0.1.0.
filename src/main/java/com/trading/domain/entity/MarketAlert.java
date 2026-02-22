package com.trading.domain.entity;

import com.trading.domain.enums.MarketAlertStrategyType;
import com.trading.domain.enums.MarketAlertType;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "market_alerts")
public class MarketAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 4)
    private MarketAlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false, length = 10)
    private MarketAlertStrategyType strategyType;

    @Column(name = "rsi_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal rsiValue;

    @Column(name = "stoch_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal stochValue;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays;

    @Column(name = "trigger_date", nullable = false)
    private LocalDate triggerDate;

    @Column(name = "is_viewed", nullable = false)
    private Boolean viewed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public MarketAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(MarketAlertType alertType) {
        this.alertType = alertType;
    }

    public MarketAlertStrategyType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(MarketAlertStrategyType strategyType) {
        this.strategyType = strategyType;
    }

    public BigDecimal getRsiValue() {
        return rsiValue;
    }

    public void setRsiValue(BigDecimal rsiValue) {
        this.rsiValue = rsiValue;
    }

    public BigDecimal getStochValue() {
        return stochValue;
    }

    public void setStochValue(BigDecimal stochValue) {
        this.stochValue = stochValue;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public LocalDate getTriggerDate() {
        return triggerDate;
    }

    public void setTriggerDate(LocalDate triggerDate) {
        this.triggerDate = triggerDate;
    }

    public Boolean getViewed() {
        return viewed;
    }

    public void setViewed(Boolean viewed) {
        this.viewed = viewed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
