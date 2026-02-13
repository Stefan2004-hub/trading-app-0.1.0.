package com.trading.domain.entity;

import com.trading.domain.enums.TransactionType;
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
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 4)
    private TransactionType transactionType;

    @Column(name = "gross_amount", nullable = false, precision = 20, scale = 18)
    private BigDecimal grossAmount;

    @Column(name = "fee_amount", precision = 20, scale = 18)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", length = 10)
    private String feeCurrency;

    @Column(name = "net_amount", nullable = false, precision = 20, scale = 18)
    private BigDecimal netAmount;

    @Column(name = "unit_price_usd", nullable = false, precision = 20, scale = 18)
    private BigDecimal unitPriceUsd;

    @Column(name = "total_spent_usd", nullable = false, precision = 20, scale = 18)
    private BigDecimal totalSpentUsd;

    @Column(name = "realized_pnl", precision = 20, scale = 18)
    private BigDecimal realizedPnl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_date")
    private OffsetDateTime transactionDate;

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

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getFeeCurrency() {
        return feeCurrency;
    }

    public void setFeeCurrency(String feeCurrency) {
        this.feeCurrency = feeCurrency;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getUnitPriceUsd() {
        return unitPriceUsd;
    }

    public void setUnitPriceUsd(BigDecimal unitPriceUsd) {
        this.unitPriceUsd = unitPriceUsd;
    }

    public BigDecimal getTotalSpentUsd() {
        return totalSpentUsd;
    }

    public void setTotalSpentUsd(BigDecimal totalSpentUsd) {
        this.totalSpentUsd = totalSpentUsd;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public OffsetDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(OffsetDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
