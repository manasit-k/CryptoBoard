package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.port.in.GetPriceHistoryUseCase;
import com.demo.crypto.CryptoBoard.application.port.out.LoadPriceHistoryPort;
import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Use case implementation: โหลดประวัติราคาจาก port
 */
@Service
public class GetPriceHistoryService implements GetPriceHistoryUseCase {

    private static final int MAX_LIMIT = 1000;
    private static final int DEFAULT_LIMIT = 100;

    private final LoadPriceHistoryPort loadPriceHistoryPort;

    public GetPriceHistoryService(LoadPriceHistoryPort loadPriceHistoryPort) {
        this.loadPriceHistoryPort = loadPriceHistoryPort;
    }

    @Override
    public Flux<PriceSnapshot> findHistoryBySymbol(String symbol, int limit) {
        int safeLimit = limit <= 0 || limit > MAX_LIMIT ? DEFAULT_LIMIT : limit;
        return loadPriceHistoryPort.findHistoryBySymbol(symbol, safeLimit);
    }

    @Override
    public Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to) {
        return loadPriceHistoryPort.findHistoryBySymbolBetween(symbol, from, to);
    }
}
