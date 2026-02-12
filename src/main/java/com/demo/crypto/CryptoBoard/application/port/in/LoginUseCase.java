package com.demo.crypto.CryptoBoard.application.port.in;

import com.demo.crypto.CryptoBoard.application.dto.LoginRequest;
import com.demo.crypto.CryptoBoard.application.dto.LoginResponse;
import reactor.core.publisher.Mono;

/**
 * Use case: ล็อกอินด้วย username/password ได้ JWT กลับไป
 */
public interface LoginUseCase {

    /**
     * ยืนยันตัวตนแล้วสร้าง token
     * @return LoginResponse ถ้าสำเร็จ, empty ถ้า username/password ไม่ตรง
     */
    Mono<LoginResponse> login(LoginRequest request);
}
