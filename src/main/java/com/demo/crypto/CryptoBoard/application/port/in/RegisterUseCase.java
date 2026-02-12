package com.demo.crypto.CryptoBoard.application.port.in;

import com.demo.crypto.CryptoBoard.application.dto.RegisterRequest;
import reactor.core.publisher.Mono;

/**
 * Use case: ลงทะเบียนผู้ใช้ใหม่
 * @return empty on success, error signal ถ้า username ซ้ำหรือข้อมูลไม่ถูกต้อง
 */
public interface RegisterUseCase {

    Mono<Void> register(RegisterRequest request);
}
