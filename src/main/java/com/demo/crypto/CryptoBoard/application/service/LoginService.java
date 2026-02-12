package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.dto.LoginRequest;
import com.demo.crypto.CryptoBoard.application.dto.LoginResponse;
import com.demo.crypto.CryptoBoard.application.port.in.LoginUseCase;
import com.demo.crypto.CryptoBoard.application.port.out.LoadUserPort;
import com.demo.crypto.CryptoBoard.application.port.out.PasswordEncoderPort;
import com.demo.crypto.CryptoBoard.application.port.out.TokenProviderPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Use case implementation: ล็อกอิน — ตรวจรหัสผ่านแล้วออก JWT
 */
@Service
public class LoginService implements LoginUseCase {

    private final LoadUserPort loadUserPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;

    public LoginService(
            LoadUserPort loadUserPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenProviderPort tokenProviderPort) {
        this.loadUserPort = loadUserPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    @Override
    public Mono<LoginResponse> login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getPassword() == null) {
            return Mono.empty();
        }
        return loadUserPort.findByUsername(request.getUsername().trim())
                .filter(user -> passwordEncoderPort.matches(request.getPassword(), user.getPasswordHash()))
                .map(user -> {
                    String token = tokenProviderPort.createToken(user.getUsername());
                    return new LoginResponse(token, user.getUsername());
                })
                .switchIfEmpty(Mono.empty());
    }
}
