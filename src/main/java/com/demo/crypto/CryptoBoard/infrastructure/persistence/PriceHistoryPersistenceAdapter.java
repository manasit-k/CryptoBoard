package com.demo.crypto.CryptoBoard.infrastructure.persistence;

import com.demo.crypto.CryptoBoard.application.port.out.LoadPriceHistoryPort;
import com.demo.crypto.CryptoBoard.application.port.out.SavePriceHistoryPort;
import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Adapter: ใช้ DatabaseClient เขียน SQL ตรงๆ (non-blocking)
 * ไม่พึ่ง Spring Data R2DBC Repository — หลีกเลี่ยงปัญหา dialect/bind-marker
 */
@Component
public class PriceHistoryPersistenceAdapter implements SavePriceHistoryPort, LoadPriceHistoryPort {

    private final DatabaseClient db;

    public PriceHistoryPersistenceAdapter(DatabaseClient db) {
        this.db = db;
    }

    @Override
    public Mono<PriceSnapshot> save(PriceSnapshot snapshot) {
        return db.sql("INSERT INTO price_snapshot (symbol, price, change_24h, volume_24h, recorded_at) VALUES (?, ?, ?, ?, ?)")
                .bind(0, snapshot.getSymbol())
                .bind(1, snapshot.getPrice())
                .bind(2, snapshot.getChange24h() != null ? snapshot.getChange24h() : "")
                .bind(3, snapshot.getVolume24h() != null ? snapshot.getVolume24h() : "")
                .bind(4, snapshot.getRecordedAt())
                .fetch()
                .rowsUpdated()
                .thenReturn(snapshot);
    }

    @Override
    public Flux<PriceSnapshot> saveAll(Flux<PriceSnapshot> snapshots) {
        return snapshots
                .flatMap(this::save)
                .onErrorResume(e -> {
                    System.err.println("PriceHistoryPersistenceAdapter save error: " + e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Flux<PriceSnapshot> findHistoryBySymbol(String symbol, int limit) {
        return db.sql("SELECT * FROM price_snapshot WHERE symbol = ? ORDER BY recorded_at DESC LIMIT ?")
                .bind(0, symbol)
                .bind(1, limit)
                .map(row -> PriceSnapshot.withId(
                        row.get("id", Long.class),
                        row.get("symbol", String.class),
                        row.get("price", String.class),
                        row.get("change_24h", String.class),
                        row.get("volume_24h", String.class),
                        row.get("recorded_at", Instant.class)
                ))
                .all();
    }

    @Override
    public Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to) {
        return db.sql("SELECT * FROM price_snapshot WHERE symbol = ? AND recorded_at >= ? AND recorded_at <= ? ORDER BY recorded_at ASC")
                .bind(0, symbol)
                .bind(1, from)
                .bind(2, to)
                .map(row -> PriceSnapshot.withId(
                        row.get("id", Long.class),
                        row.get("symbol", String.class),
                        row.get("price", String.class),
                        row.get("change_24h", String.class),
                        row.get("volume_24h", String.class),
                        row.get("recorded_at", Instant.class)
                ))
                .all();
    }
}
