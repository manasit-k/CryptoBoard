package com.demo.crypto.CryptoBoard.domain;

import java.time.Instant;

/**
 * Domain entity สำหรับ snapshot ราคา Crypto
 * ไม่พึ่ง framework (pure POJO) — ใช้ใน application และ persistence layer
 */
public class PriceSnapshot {

    private Long id;
    private final String symbol;
    private final String price;
    private final String change24h;
    private final String volume24h;
    private final Instant recordedAt;

    public PriceSnapshot(String symbol, String price, String change24h, String volume24h, Instant recordedAt) {
        this.symbol = symbol;
        this.price = price;
        this.change24h = change24h;
        this.volume24h = volume24h;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
    }

    /** สำหรับโหลดจาก persistence (มี id) */
    public static PriceSnapshot withId(Long id, String symbol, String price, String change24h, String volume24h, Instant recordedAt) {
        PriceSnapshot s = new PriceSnapshot(symbol, price, change24h, volume24h, recordedAt);
        s.id = id;
        return s;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getPrice() {
        return price;
    }

    public String getChange24h() {
        return change24h;
    }

    public String getVolume24h() {
        return volume24h;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
