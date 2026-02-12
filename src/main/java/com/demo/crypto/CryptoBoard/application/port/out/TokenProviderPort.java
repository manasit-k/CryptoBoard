package com.demo.crypto.CryptoBoard.application.port.out;

/**
 * Port สำหรับสร้าง JWT จาก username (infrastructure จะใช้ library จริง)
 */
public interface TokenProviderPort {

    String createToken(String username);
}
