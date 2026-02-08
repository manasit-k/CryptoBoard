package com.demo.crypto.CryptoBoard.dto;

import com.demo.crypto.CryptoBoard.infrastructure.external.BinanceTicker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class BinanceTickerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest(name = "symbol={0} lastPrice={1} priceChangePercent={2} volume={3}")
    @CsvSource({
            "BTCUSDT, 50000.00, 3.5, 100000",
            "ETHUSDT, 3000.00, -2.5, 500000",
            "BNBUSDT, 350.00, 1.2, 25000"
    })
    void shouldSetAndGetAllFields(String symbol, String lastPrice, String priceChangePercent, String volume) {
        BinanceTicker ticker = new BinanceTicker();
        ticker.setSymbol(symbol.trim());
        ticker.setLastPrice(lastPrice.trim());
        ticker.setPriceChangePercent(priceChangePercent.trim());
        ticker.setVolume(volume.trim());

        assertEquals(symbol.trim(), ticker.getSymbol());
        assertEquals(lastPrice.trim(), ticker.getLastPrice());
        assertEquals(priceChangePercent.trim(), ticker.getPriceChangePercent());
        assertEquals(volume.trim(), ticker.getVolume());
    }

    @ParameterizedTest(name = "symbol={0} lastPrice={1} priceChangePercent={2} volume={3}")
    @CsvSource({
            "ETHUSDT, 3000.00, -2.5, 500000",
            "BTCUSDT, 50000.00, 1.0, 200000"
    })
    void shouldDeserializeFromJson(String symbol, String lastPrice, String priceChangePercent, String volume) throws Exception {
        String json = """
                {
                    "symbol": "%s",
                    "lastPrice": "%s",
                    "priceChangePercent": "%s",
                    "volume": "%s"
                }
                """.formatted(symbol.trim(), lastPrice.trim(), priceChangePercent.trim(), volume.trim());

        BinanceTicker ticker = objectMapper.readValue(json, BinanceTicker.class);

        assertEquals(symbol.trim(), ticker.getSymbol());
        assertEquals(lastPrice.trim(), ticker.getLastPrice());
        assertEquals(priceChangePercent.trim(), ticker.getPriceChangePercent());
        assertEquals(volume.trim(), ticker.getVolume());
    }

    @Test
    void shouldIgnoreUnknownFieldsOnDeserialize() throws Exception {
        String json = """
                {
                    "symbol": "BTCUSDT",
                    "lastPrice": "50000.00",
                    "priceChangePercent": "1.0",
                    "volume": "200000",
                    "unknownField": "shouldBeIgnored"
                }
                """;

        BinanceTicker ticker = objectMapper.readValue(json, BinanceTicker.class);

        assertEquals("BTCUSDT", ticker.getSymbol());
        assertEquals("50000.00", ticker.getLastPrice());
    }

    @Test
    void shouldHandleNullFields() {
        BinanceTicker ticker = new BinanceTicker();

        assertNull(ticker.getSymbol());
        assertNull(ticker.getLastPrice());
        assertNull(ticker.getPriceChangePercent());
        assertNull(ticker.getVolume());
    }
}
