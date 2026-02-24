package com.trading.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "symbol", nullable = false, length = 10, unique = true)
    private String symbol;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "coin_gecko_id", length = 120)
    private String coinGeckoId;

    @Column(name = "gate_io_symbol", length = 20)
    private String gateIoSymbol;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoinGeckoId() {
        return coinGeckoId;
    }

    public void setCoinGeckoId(String coinGeckoId) {
        this.coinGeckoId = coinGeckoId;
    }

    public String getGateIoSymbol() {
        return gateIoSymbol;
    }

    public void setGateIoSymbol(String gateIoSymbol) {
        this.gateIoSymbol = gateIoSymbol;
    }
}
