package com.demo.crypto.CryptoBoard.service;

import com.demo.crypto.CryptoBoard.infrastructure.external.BinancePriceProvider;
import com.demo.crypto.CryptoBoard.infrastructure.external.BinanceTicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BinancePriceProviderTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private BinancePriceProvider provider;

    private final List<String> symbols = List.of("BTCUSDT", "ETHUSDT");

    @BeforeEach
    void setUp() {
        provider = new BinancePriceProvider(webClient, symbols);
    }

    @Test
    void shouldReturnExchangeNameAsBinance() {
        assertEquals("Binance", provider.getExchangeName());
    }

    @ParameterizedTest(name = "symbol={0} price={1} change24h={2} volume={3}")
    @CsvSource({
            "BTCUSDT, 50000.00, 3.5, 100000",
            "ETHUSDT, 3000.50, -2.75, 750000",
            "BNBUSDT, 350.00, 1.2, 50000"
    })
    void shouldMapTickerToCryptoPrice(String symbol, String price, String change24h, String volume) {
        BinanceTicker ticker = createTicker(symbol.trim(), price.trim(), change24h.trim(), volume.trim());
        mockWebClientCall(Mono.just(ticker));

        StepVerifier.create(provider.getPrice(symbol.trim()))
                .assertNext(cryptoPrice -> {
                    assertEquals(symbol.trim(), cryptoPrice.getSymbol());
                    assertEquals(price.trim(), cryptoPrice.getPrice());
                    assertEquals(change24h.trim(), cryptoPrice.getChange24h());
                    assertEquals(volume.trim(), cryptoPrice.getVolume24h());
                    assertTrue(cryptoPrice.getTimestamp() > 0);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyOnApiError() {
        // Mock ให้ error ตั้งแต่แรก — retry + timeout จะ fire แต่สุดท้าย onErrorResume จะส่ง empty
        mockWebClientCall(Mono.error(new RuntimeException("API Error")));

        StepVerifier.create(provider.getPrice("BTCUSDT"))
                .verifyComplete(); // ควร empty เพราะ onErrorResume
    }

    @Test
    void shouldGetAllPricesForAllSymbols() {
        BinanceTicker btcTicker = createTicker("BTCUSDT", "50000.00", "3.5", "100000");
        BinanceTicker ethTicker = createTicker("ETHUSDT", "3000.00", "-1.2", "500000");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BinanceTicker.class))
                .thenReturn(Mono.just(btcTicker))
                .thenReturn(Mono.just(ethTicker));

        StepVerifier.create(provider.getAllPrices())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void shouldFilterOutNullPricesInGetAllPrices() {
        BinanceTicker tickerWithNullPrice = createTicker("BTCUSDT", null, "1.0", "100000");
        BinanceTicker validTicker = createTicker("ETHUSDT", "3000.00", "1.0", "500000");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BinanceTicker.class))
                .thenReturn(Mono.just(tickerWithNullPrice))
                .thenReturn(Mono.just(validTicker));

        StepVerifier.create(provider.getAllPrices())
                .assertNext(price -> {
                    assertEquals("ETHUSDT", price.getSymbol());
                    assertEquals("3000.00", price.getPrice());
                })
                .verifyComplete();
    }

    // ── Helper methods ──

    private BinanceTicker createTicker(String symbol, String price, String changePercent, String volume) {
        BinanceTicker ticker = new BinanceTicker();
        ticker.setSymbol(symbol);
        ticker.setLastPrice(price);
        ticker.setPriceChangePercent(changePercent);
        ticker.setVolume(volume);
        return ticker;
    }

    @SuppressWarnings("unchecked")
    private void mockWebClientCall(Mono<BinanceTicker> response) {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(BinanceTicker.class)).thenReturn(response);
    }
}
