package com.demo.crypto.CryptoBoard.controller;

import com.demo.crypto.CryptoBoard.application.dto.LoginRequest;
import com.demo.crypto.CryptoBoard.application.dto.LoginResponse;
import com.demo.crypto.CryptoBoard.application.dto.RegisterRequest;
import com.demo.crypto.CryptoBoard.application.port.in.LoginUseCase;
import com.demo.crypto.CryptoBoard.application.port.in.RegisterUseCase;
import com.demo.crypto.CryptoBoard.interfaces.web.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private LoginUseCase loginUseCase;
    @Mock
    private RegisterUseCase registerUseCase;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(loginUseCase, registerUseCase);
    }

    @Test
    void shouldReturnLoginResponseWhenLoginSuccess() {
        LoginResponse response = new LoginResponse("jwt.token", "demo");
        when(loginUseCase.login(any(LoginRequest.class))).thenReturn(Mono.just(response));

        StepVerifier.create(authController.login(new LoginRequest("demo", "demo123")))
                .expectNext(response)
                .verifyComplete();

        verify(loginUseCase).login(any(LoginRequest.class));
    }

    @Test
    void shouldErrorWhenLoginReturnsEmpty() {
        when(loginUseCase.login(any(LoginRequest.class))).thenReturn(Mono.empty());

        StepVerifier.create(authController.login(new LoginRequest("bad", "bad")))
                .expectError(AuthController.UnauthorizedException.class)
                .verify();
    }

    @Test
    void shouldCompleteWhenRegisterSuccess() {
        when(registerUseCase.register(any(RegisterRequest.class))).thenReturn(Mono.empty());

        StepVerifier.create(authController.register(new RegisterRequest("newuser", "pass1234")))
                .verifyComplete();

        verify(registerUseCase).register(any(RegisterRequest.class));
    }

    @Test
    void shouldPropagateErrorWhenRegisterFails() {
        when(registerUseCase.register(any(RegisterRequest.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("username already taken")));

        StepVerifier.create(authController.register(new RegisterRequest("existing", "pass1234")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
