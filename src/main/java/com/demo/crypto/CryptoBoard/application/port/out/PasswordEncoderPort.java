package com.demo.crypto.CryptoBoard.application.port.out;

/**
 * Port สำหรับเข้ารหัสและตรวจสอบรหัสผ่าน (infrastructure ใช้ BCrypt)
 */
public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
