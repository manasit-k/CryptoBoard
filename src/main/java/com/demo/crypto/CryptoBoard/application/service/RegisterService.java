package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.port.in.RegisterUseCase;
import com.demo.crypto.CryptoBoard.application.port.out.LoadUserPort;
import com.demo.crypto.CryptoBoard.application.port.out.PasswordEncoderPort;
import com.demo.crypto.CryptoBoard.application.port.out.SaveUserPort;
import com.demo.crypto.CryptoBoard.domain.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case implementation: ลงทะเบียนผู้ใช้ใหม่ (hash รหัสผ่านแล้ว save)
 */
@Service
public class RegisterService implements RegisterUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final SaveUserPort saveUserPort;

    public RegisterService(
            LoadUserPort loadUserPort,
            PasswordEncoderPort passwordEncoderPort,
            SaveUserPort saveUserPort) {
        this.loadUserPort = loadUserPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.saveUserPort = saveUserPort;
    }

    @Override
    public Mono<Void> register(com.demo.crypto.CryptoBoard.application.dto.RegisterRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return Mono.error(new IllegalArgumentException("username and password required"));
        }
        String username = request.getUsername().trim();
        if (username.isEmpty()) {
            return Mono.error(new IllegalArgumentException("username required"));
        }
        if (request.getPassword().length() < 4) {
            return Mono.error(new IllegalArgumentException("password must be at least 4 characters"));
        }
        return loadUserPort.findByUsername(username)
                .hasElement()
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new IllegalArgumentException("username already taken"));
                    }
                    String hash = passwordEncoderPort.encode(request.getPassword());
                    User user = User.withoutId(username, hash);
                    return saveUserPort.save(user).then();
                });
    }
}
