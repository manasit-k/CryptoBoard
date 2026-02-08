package com.demo.crypto.CryptoBoard.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * โมเดลตอบกลับจาก Binance API — ใช้เฉพาะใน infrastructure
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceTicker {
    @JsonProperty("symbol")
    private String symbol;

    // ใน 24hr ticker ของ Binance ไม่มีฟิลด์ "price"
    // ใช้ "lastPrice" เป็นราคาปัจจุบันแทน
    @JsonProperty("lastPrice")
    private String lastPrice;

    @JsonProperty("priceChangePercent")
    private String priceChangePercent;

    @JsonProperty("volume")
    private String volume;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(String lastPrice) {
        this.lastPrice = lastPrice;
    }

    public String getPriceChangePercent() {
        return priceChangePercent;
    }

    public void setPriceChangePercent(String priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }
}
