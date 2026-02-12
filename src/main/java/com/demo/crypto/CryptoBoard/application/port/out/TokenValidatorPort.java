package com.demo.crypto.CryptoBoard.application.port.out;

import reactor.core.publisher.Mono;

/**
 * Port สำหรับตรวจสอบ JWT และดึง username ออกมา (ใช้ใน filter)
 */
public interface TokenValidatorPort {

    /**
     * ถ้า token ถูกต้อง return username ไม่เช่นนั้น return empty
     */
    Mono<String> validateAndGetUsername(String token);
}
