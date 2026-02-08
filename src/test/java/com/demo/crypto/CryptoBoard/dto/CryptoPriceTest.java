package com.demo.crypto.CryptoBoard.dto;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class CryptoPriceTest {

    @ParameterizedTest(name = "symbol={0} price={1} change24h={2} volume24h={3}")
    @CsvSource({
            "BTCUSDT, 50000.00, ,",
            "ETHUSDT, 3000.00, 5.25, 1000000",
            "SOLUSDT, 150.00, -1.5, 500000"
    })
    void shouldCreateWithConstructor(String symbol, String price, String change24h, String volume24h) {
        boolean useBasicConstructor = (change24h == null || change24h.isBlank()) && (volume24h == null || volume24h.isBlank());
        CryptoPrice cryptoPrice = useBasicConstructor
                ? new CryptoPrice(symbol.trim(), price.trim())
                : new CryptoPrice(symbol.trim(), price.trim(), change24h.trim(), volume24h.trim());

        assertEquals(symbol.trim(), cryptoPrice.getSymbol());
        assertEquals(price.trim(), cryptoPrice.getPrice());
        if (change24h == null || change24h.isBlank()) {
            assertNull(cryptoPrice.getChange24h());
        } else {
            assertEquals(change24h.trim(), cryptoPrice.getChange24h());
        }
        if (volume24h == null || volume24h.isBlank()) {
            assertNull(cryptoPrice.getVolume24h());
        } else {
            assertEquals(volume24h.trim(), cryptoPrice.getVolume24h());
        }
        assertTrue(cryptoPrice.getTimestamp() > 0);
    }

    @Test
    void shouldSetAndGetAllFields() {
        CryptoPrice price = new CryptoPrice("BTCUSDT", "50000.00");

        price.setSymbol("ETHUSDT");
        price.setPrice("3000.00");
        price.setChange24h("-2.5");
        price.setVolume24h("500000");
        price.setTimestamp(1234567890L);

        assertEquals("ETHUSDT", price.getSymbol());
        assertEquals("3000.00", price.getPrice());
        assertEquals("-2.5", price.getChange24h());
        assertEquals("500000", price.getVolume24h());
        assertEquals(1234567890L, price.getTimestamp());
    }

    @Test
    void shouldGenerateTimestampAutomatically() {
        long before = System.currentTimeMillis();
        CryptoPrice price = new CryptoPrice("BTCUSDT", "50000.00");
        long after = System.currentTimeMillis();

        assertTrue(price.getTimestamp() >= before);
        assertTrue(price.getTimestamp() <= after);
    }

    @ParameterizedTest(name = "symbol={0} price={1} change24h={2} volume24h={3}")
    @CsvSource({
            "BTCUSDT, 50000.00, 3.5, 100000",
            "ETHUSDT, 3000.00, -2.0, 500000"
    })
    void shouldReturnCorrectToString(String symbol, String price, String change24h, String volume24h) {
        CryptoPrice cryptoPrice = new CryptoPrice(symbol.trim(), price.trim(), change24h.trim(), volume24h.trim());
        String result = cryptoPrice.toString();
        assertTrue(result.contains(symbol.trim()));
        assertTrue(result.contains(price.trim()));
        assertTrue(result.contains(change24h.trim()));
        assertTrue(result.contains(volume24h.trim()));
    }
}
