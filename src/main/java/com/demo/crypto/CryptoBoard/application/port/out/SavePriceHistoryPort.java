package com.demo.crypto.CryptoBoard.application.port.out;

import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port สำหรับบันทึกประวัติราคาลง persistence
 */
public interface SavePriceHistoryPort {

    Mono<PriceSnapshot> save(PriceSnapshot snapshot);

    Flux<PriceSnapshot> saveAll(Flux<PriceSnapshot> snapshots);
}
