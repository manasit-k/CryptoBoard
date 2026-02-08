package com.demo.crypto.CryptoBoard.interfaces.web;

import com.demo.crypto.CryptoBoard.application.dto.CryptoPrice;
import com.demo.crypto.CryptoBoard.application.port.in.GetCryptoPricesUseCase;
import com.demo.crypto.CryptoBoard.application.port.in.GetPriceHistoryUseCase;
import com.demo.crypto.CryptoBoard.domain.PriceSnapshot;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * HTTP adapter: รับ request แล้วเรียก use case — ไม่มี business logic
 * ทุก endpoint เป็น non-blocking (Mono/Flux) ไม่มี streaming/SSE
 */
@RestController
@RequestMapping("/api/crypto")
@CrossOrigin(origins = "*")
public class CryptoController {

    private final GetCryptoPricesUseCase getCryptoPricesUseCase;
    private final GetPriceHistoryUseCase getPriceHistoryUseCase;

    public CryptoController(
            GetCryptoPricesUseCase getCryptoPricesUseCase,
            GetPriceHistoryUseCase getPriceHistoryUseCase) {
        this.getCryptoPricesUseCase = getCryptoPricesUseCase;
        this.getPriceHistoryUseCase = getPriceHistoryUseCase;
    }

    @GetMapping("/prices")
    public Flux<CryptoPrice> getAllPrices() {
        return getCryptoPricesUseCase.getAllPrices();
    }

    @GetMapping("/price/{symbol}")
    public Mono<CryptoPrice> getPrice(@PathVariable String symbol) {
        return getCryptoPricesUseCase.getPrice(symbol.toUpperCase() + "USDT");
    }

    @GetMapping("/symbols")
    public List<String> getAvailableSymbols() {
        return getCryptoPricesUseCase.getAvailableSymbols();
    }

    @GetMapping("/history/{symbol}")
    public Flux<PriceSnapshot> getPriceHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "100") int limit) {
        return getPriceHistoryUseCase.findHistoryBySymbol(symbol.toUpperCase() + "USDT", limit);
    }

    @GetMapping("/history/{symbol}/range")
    public Flux<PriceSnapshot> getPriceHistoryRange(
            @PathVariable String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return getPriceHistoryUseCase.findHistoryBySymbolBetween(symbol.toUpperCase() + "USDT", from, to);
    }
}
