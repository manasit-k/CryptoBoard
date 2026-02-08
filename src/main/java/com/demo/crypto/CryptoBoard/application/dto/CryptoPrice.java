package com.demo.crypto.CryptoBoard.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO สำหรับราคา Crypto ที่ stream / API ส่งออก
 * อยู่ใน application layer (output model ของ use case)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CryptoPrice {
    private String symbol;
    private String price;
    private String change24h;
    private String volume24h;
    private long timestamp;

    public CryptoPrice() {
        this.timestamp = System.currentTimeMillis();
    }

    public CryptoPrice(String symbol, String price) {
        this.symbol = symbol;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    public CryptoPrice(String symbol, String price, String change24h, String volume24h) {
        this.symbol = symbol;
        this.price = price;
        this.change24h = change24h;
        this.volume24h = volume24h;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getChange24h() {
        return change24h;
    }

    public void setChange24h(String change24h) {
        this.change24h = change24h;
    }

    public String getVolume24h() {
        return volume24h;
    }

    public void setVolume24h(String volume24h) {
        this.volume24h = volume24h;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "CryptoPrice{" +
                "symbol='" + symbol + '\'' +
                ", price='" + price + '\'' +
                ", change24h='" + change24h + '\'' +
                ", volume24h='" + volume24h + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
