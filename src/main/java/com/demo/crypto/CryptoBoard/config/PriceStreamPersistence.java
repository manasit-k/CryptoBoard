package com.demo.crypto.CryptoBoard.config;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import com.demo.crypto.CryptoBoard.application.port.in.GetCryptoPricesUseCase;
import com.demo.crypto.CryptoBoard.application.port.out.SavePriceHistoryPort;
import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * บันทึกราคาลง DB เป็นระยะ (scheduled) แบบ non-blocking
 * เรียก getAllPrices() ครั้งเดียวต่อรอบ ไม่มี streaming
 */
@Component
public class PriceStreamPersistence {

    private final GetCryptoPricesUseCase getCryptoPricesUseCase;
    private final SavePriceHistoryPort savePriceHistoryPort;
    private final boolean enabled;

    public PriceStreamPersistence(
            GetCryptoPricesUseCase getCryptoPricesUseCase,
            SavePriceHistoryPort savePriceHistoryPort,
            @Value("${crypto.history.enabled:true}") boolean enabled) {
        this.getCryptoPricesUseCase = getCryptoPricesUseCase;
        this.savePriceHistoryPort = savePriceHistoryPort;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${crypto.persistence.interval-ms:5000}")
    public void persistPrices() {
        if (!enabled) return;
        getCryptoPricesUseCase.getAllPrices()
                .map(this::toSnapshot)
                .collectList()
                .filter(list -> !list.isEmpty())
                .flatMapMany(list -> savePriceHistoryPort.saveAll(Flux.fromIterable(list)))
                .subscribe(
                        snapshot -> { },
                        err -> System.err.println("PriceStreamPersistence error: " + err.getMessage())
                );
    }

    private PriceSnapshot toSnapshot(CryptoPrice p) {
        Instant recordedAt = p.getTimestamp() > 0
                ? Instant.ofEpochMilli(p.getTimestamp())
                : Instant.now();
        return new PriceSnapshot(
                p.getSymbol(),
                p.getPrice(),
                p.getChange24h(),
                p.getVolume24h(),
                recordedAt
        );
    }
}
