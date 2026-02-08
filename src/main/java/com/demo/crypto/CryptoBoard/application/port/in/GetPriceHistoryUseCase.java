package com.demo.crypto.CryptoBoard.application.port.in;

import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Use case: ดึงประวัติราคาจาก persistence
 */
public interface GetPriceHistoryUseCase {

    Flux<PriceSnapshot> findHistoryBySymbol(String symbol, int limit);

    Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to);
}
