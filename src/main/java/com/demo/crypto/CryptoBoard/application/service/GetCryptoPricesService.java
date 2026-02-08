package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import com.demo.crypto.CryptoBoard.application.port.in.GetCryptoPricesUseCase;
import com.demo.crypto.CryptoBoard.application.port.out.CryptoPriceProviderPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use case implementation: ดึงราคาจาก port แบบ non-blocking (one-shot)
 */
@Service
public class GetCryptoPricesService implements GetCryptoPricesUseCase {

    private final CryptoPriceProviderPort priceProvider;
    private final List<String> symbols;

    public GetCryptoPricesService(
            CryptoPriceProviderPort priceProvider,
            @Value("${crypto.symbols}") List<String> symbols) {
        this.priceProvider = priceProvider;
        this.symbols = symbols;
    }

    @Override
    public Flux<CryptoPrice> getAllPrices() {
        return priceProvider.getAllPrices();
    }

    @Override
    public Mono<CryptoPrice> getPrice(String symbol) {
        return priceProvider.getPrice(symbol);
    }

    @Override
    public List<String> getAvailableSymbols() {
        return symbols;
    }
}
