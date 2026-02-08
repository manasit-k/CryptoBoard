package com.demo.crypto.CryptoBoard.application.port.in;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use case: ดึงราคา Crypto แบบ non-blocking (one-shot ไม่มี streaming)
 */
public interface GetCryptoPricesUseCase {

    Flux<CryptoPrice> getAllPrices();

    Mono<CryptoPrice> getPrice(String symbol);

    List<String> getAvailableSymbols();
}
