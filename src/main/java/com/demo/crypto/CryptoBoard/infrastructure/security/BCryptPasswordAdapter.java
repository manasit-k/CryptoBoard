package com.demo.crypto.CryptoBoard.infrastructure.security;

import com.demo.crypto.CryptoBoard.application.port.out.PasswordEncoderPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter: เข้ารหัสและตรวจสอบรหัสผ่านด้วย BCrypt
 */
@Component
public class BCryptPasswordAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
