package com.demo.crypto.CryptoBoard.application.port.out;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Port สำหรับดึงราคา Crypto จาก exchange ภายนอก (Binance, CoinGecko, etc.)
 */
public interface CryptoPriceProviderPort {

    Mono<CryptoPrice> getPrice(String symbol);

    Flux<CryptoPrice> getAllPrices();

    String getExchangeName();
}
