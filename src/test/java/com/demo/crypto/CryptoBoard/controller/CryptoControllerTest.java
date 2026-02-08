package com.demo.crypto.CryptoBoard.controller;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import com.demo.crypto.CryptoBoard.application.port.in.GetCryptoPricesUseCase;
import com.demo.crypto.CryptoBoard.application.port.in.GetPriceHistoryUseCase;
import com.demo.crypto.CryptoBoard.interfaces.web.CryptoController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class CryptoControllerTest {

    @Mock
    private GetCryptoPricesUseCase getCryptoPricesUseCase;

    @Mock
    private GetPriceHistoryUseCase getPriceHistoryUseCase;

    private CryptoController controller;

    @BeforeEach
    void setUp() {
        controller = new CryptoController(getCryptoPricesUseCase, getPriceHistoryUseCase);
    }

    @Test
    void shouldGetAllPrices() {
        CryptoPrice btc = new CryptoPrice("BTCUSDT", "50000.00");
        CryptoPrice eth = new CryptoPrice("ETHUSDT", "3000.00");

        when(getCryptoPricesUseCase.getAllPrices()).thenReturn(Flux.just(btc, eth));

        StepVerifier.create(controller.getAllPrices())
                .expectNext(btc)
                .expectNext(eth)
                .verifyComplete();

        verify(getCryptoPricesUseCase, times(1)).getAllPrices();
    }

    @ParameterizedTest(name = "input={0} -> useCaseSymbol={1} price={2}")
    @CsvSource({
            "btc, BTCUSDT, 50000.00",
            "eth, ETHUSDT, 3000.00",
            "bnb, BNBUSDT, 350.00"
    })
    void shouldGetPriceAndConvertSymbolToUpperCase(String inputSymbol, String expectedUseCaseSymbol, String expectedPrice) {
        CryptoPrice expected = new CryptoPrice(expectedUseCaseSymbol, expectedPrice);
        when(getCryptoPricesUseCase.getPrice(expectedUseCaseSymbol)).thenReturn(Mono.just(expected));

        StepVerifier.create(controller.getPrice(inputSymbol))
                .expectNext(expected)
                .verifyComplete();

        verify(getCryptoPricesUseCase, times(1)).getPrice(expectedUseCaseSymbol);
    }

    @Test
    void shouldGetAvailableSymbols() {
        List<String> symbols = List.of("BTCUSDT", "ETHUSDT");

        when(getCryptoPricesUseCase.getAvailableSymbols()).thenReturn(symbols);

        List<String> result = controller.getAvailableSymbols();

        assertEquals(2, result.size());
        assertEquals("BTCUSDT", result.get(0));
        assertEquals("ETHUSDT", result.get(1));
    }

    @Test
    void shouldReturnEmptyWhenPriceNotFound() {
        when(getCryptoPricesUseCase.getPrice("UNKNOWNUSDT")).thenReturn(Mono.empty());

        StepVerifier.create(controller.getPrice("unknown"))
                .verifyComplete();
    }
}
