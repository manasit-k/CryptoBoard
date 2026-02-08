package com.demo.crypto.CryptoBoard.infrastructure.persistence;

import com.demo.crypto.CryptoBoard.application.port.out.LoadPriceHistoryPort;
import com.demo.crypto.CryptoBoard.application.port.out.SavePriceHistoryPort;
import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Adapter: ใช้ JdbcTemplate เขียน SQL ตรงๆ
 * wrap ด้วย Mono.fromCallable + Schedulers.boundedElastic เพื่อไม่บล็อก event loop
 */
@Component
public class PriceHistoryPersistenceAdapter implements SavePriceHistoryPort, LoadPriceHistoryPort {

    private final JdbcTemplate jdbc;

    public PriceHistoryPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Mono<PriceSnapshot> save(PriceSnapshot snapshot) {
        return Mono.fromCallable(() -> {
            jdbc.update(
                    "INSERT INTO price_snapshot (symbol, price, change_24h, volume_24h, recorded_at) VALUES (?, ?, ?, ?, ?)",
                    snapshot.getSymbol(),
                    snapshot.getPrice(),
                    snapshot.getChange24h() != null ? snapshot.getChange24h() : "",
                    snapshot.getVolume24h() != null ? snapshot.getVolume24h() : "",
                    Timestamp.from(snapshot.getRecordedAt())
            );
            return snapshot;
        }).subscribeOn(Schedulers.boundedElastic());
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
        return Mono.fromCallable(() -> {
            List<PriceSnapshot> list = jdbc.query(
                    "SELECT * FROM price_snapshot WHERE symbol = ? ORDER BY recorded_at DESC LIMIT ?",
                    (rs, rowNum) -> PriceSnapshot.withId(
                            rs.getLong("id"),
                            rs.getString("symbol"),
                            rs.getString("price"),
                            rs.getString("change_24h"),
                            rs.getString("volume_24h"),
                            rs.getTimestamp("recorded_at").toInstant()
                    ),
                    symbol, limit
            );
            return list;
        }).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to) {
        return Mono.fromCallable(() -> {
            List<PriceSnapshot> list = jdbc.query(
                    "SELECT * FROM price_snapshot WHERE symbol = ? AND recorded_at >= ? AND recorded_at <= ? ORDER BY recorded_at ASC",
                    (rs, rowNum) -> PriceSnapshot.withId(
                            rs.getLong("id"),
                            rs.getString("symbol"),
                            rs.getString("price"),
                            rs.getString("change_24h"),
                            rs.getString("volume_24h"),
                            rs.getTimestamp("recorded_at").toInstant()
                    ),
                    symbol, Timestamp.from(from), Timestamp.from(to)
            );
            return list;
        }).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable);
    }
}
