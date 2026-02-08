package com.demo.crypto.CryptoBoard.infrastructure.external;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import com.demo.crypto.CryptoBoard.application.port.out.CryptoPriceProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Adapter: ดึงราคาจาก Binance API — implement CryptoPriceProviderPort
 */
@Component
public class BinancePriceProvider implements CryptoPriceProviderPort {

    private static final String BASE_URL = "https://api.binance.com/api/v3/ticker/24hr";

    private final WebClient webClient;
    private final List<String> symbols;

    public BinancePriceProvider(
            WebClient webClient,
            @Value("${crypto.symbols}") List<String> symbols) {
        this.webClient = webClient;
        this.symbols = symbols;
    }

    @Override
    public Mono<CryptoPrice> getPrice(String symbol) {
        return webClient.get()
                .uri(BASE_URL + "?symbol=" + symbol)
                .retrieve()
                .bodyToMono(BinanceTicker.class)
                .map(this::mapToCryptoPrice)
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(3)))
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(error -> {
                    System.err.println("Error fetching price for " + symbol + ": " + error.getClass().getName() + " - " + error.getMessage());
                    error.printStackTrace();
                    return Mono.empty();
                });
    }

    @Override
    public Flux<CryptoPrice> getAllPrices() {
        return Flux.fromIterable(symbols)
                .flatMap(this::getPrice)
                .filter(price -> price != null && price.getPrice() != null);
    }

    @Override
    public String getExchangeName() {
        return "Binance";
    }

    private CryptoPrice mapToCryptoPrice(BinanceTicker ticker) {
        CryptoPrice cryptoPrice = new CryptoPrice(
                ticker.getSymbol(),
                ticker.getLastPrice()
        );
        cryptoPrice.setChange24h(ticker.getPriceChangePercent());
        cryptoPrice.setVolume24h(ticker.getVolume());
        return cryptoPrice;
    }
}
