package com.demo.crypto.CryptoBoard.application.port.out;

import com.demo.crypto.CryptoBoard.domain.User;
import reactor.core.publisher.Mono;

/**
 * Port สำหรับบันทึกผู้ใช้ใหม่
 */
public interface SaveUserPort {

    Mono<User> save(User user);
}
