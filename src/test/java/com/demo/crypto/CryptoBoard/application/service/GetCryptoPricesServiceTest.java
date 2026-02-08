package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import com.demo.crypto.CryptoBoard.application.port.out.CryptoPriceProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCryptoPricesServiceTest {

    @Mock
    private CryptoPriceProviderPort priceProvider;

    private GetCryptoPricesService getCryptoPricesService;

    private final List<String> symbols = List.of("BTCUSDT", "ETHUSDT", "BNBUSDT");

    @BeforeEach
    void setUp() {
        getCryptoPricesService = new GetCryptoPricesService(priceProvider, symbols);
    }

    @Test
    void shouldGetAllPricesDelegateToProvider() {
        CryptoPrice btc = new CryptoPrice("BTCUSDT", "50000.00");
        CryptoPrice eth = new CryptoPrice("ETHUSDT", "3000.00");

        when(priceProvider.getAllPrices()).thenReturn(Flux.just(btc, eth));

        StepVerifier.create(getCryptoPricesService.getAllPrices())
                .expectNext(btc)
                .expectNext(eth)
                .verifyComplete();

        verify(priceProvider, times(1)).getAllPrices();
    }

    @Test
    void shouldGetPriceDelegateToProvider() {
        CryptoPrice btc = new CryptoPrice("BTCUSDT", "50000.00");

        when(priceProvider.getPrice("BTCUSDT")).thenReturn(Mono.just(btc));

        StepVerifier.create(getCryptoPricesService.getPrice("BTCUSDT"))
                .expectNext(btc)
                .verifyComplete();

        verify(priceProvider, times(1)).getPrice("BTCUSDT");
    }

    @Test
    void shouldReturnAvailableSymbols() {
        List<String> result = getCryptoPricesService.getAvailableSymbols();

        assertEquals(3, result.size());
        assertEquals("BTCUSDT", result.get(0));
        assertEquals("ETHUSDT", result.get(1));
        assertEquals("BNBUSDT", result.get(2));
    }

    @Test
    void shouldReturnEmptyWhenProviderReturnsEmpty() {
        when(priceProvider.getPrice("UNKNOWN")).thenReturn(Mono.empty());

        StepVerifier.create(getCryptoPricesService.getPrice("UNKNOWN"))
                .verifyComplete();
    }
}
