package com.demo.crypto.CryptoBoard.application.port.out;

import com.demo.crypto.CryptoBoard.domain.User;
import reactor.core.publisher.Mono;

/**
 * Port สำหรับโหลดผู้ใช้จาก persistence (ตาม username)
 */
public interface LoadUserPort {

    Mono<User> findByUsername(String username);
}
