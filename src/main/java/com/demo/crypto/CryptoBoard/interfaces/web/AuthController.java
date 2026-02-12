package com.demo.crypto.CryptoBoard.interfaces.web;

import com.demo.crypto.CryptoBoard.application.dto.LoginRequest;
import com.demo.crypto.CryptoBoard.application.dto.LoginResponse;
import com.demo.crypto.CryptoBoard.application.dto.RegisterRequest;
import com.demo.crypto.CryptoBoard.application.port.in.LoginUseCase;
import com.demo.crypto.CryptoBoard.application.port.in.RegisterUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * HTTP adapter: Login / Register — เรียก use case แล้ว return response
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterUseCase registerUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerUseCase = registerUseCase;
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@RequestBody LoginRequest request) {
        return loginUseCase.login(request)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid username or password")));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> register(@RequestBody RegisterRequest request) {
        return registerUseCase.register(request);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
