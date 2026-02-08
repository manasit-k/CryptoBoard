package com.demo.crypto.CryptoBoard.application.port.out;

import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Port สำหรับโหลดประวัติราคาจาก persistence
 */
public interface LoadPriceHistoryPort {

    Flux<PriceSnapshot> findHistoryBySymbol(String symbol, int limit);

    Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to);
}
