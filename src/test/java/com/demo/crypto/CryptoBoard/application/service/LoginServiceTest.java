package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.dto.LoginRequest;
import com.demo.crypto.CryptoBoard.application.dto.LoginResponse;
import com.demo.crypto.CryptoBoard.application.port.out.LoadUserPort;
import com.demo.crypto.CryptoBoard.application.port.out.PasswordEncoderPort;
import com.demo.crypto.CryptoBoard.application.port.out.TokenProviderPort;
import com.demo.crypto.CryptoBoard.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private TokenProviderPort tokenProviderPort;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(loadUserPort, passwordEncoderPort, tokenProviderPort);
    }

    @Test
    void shouldReturnLoginResponseWhenCredentialsValid() {
        User user = User.withId(1L, "demo", "$2a$10$hash");
        when(loadUserPort.findByUsername("demo")).thenReturn(Mono.just(user));
        when(passwordEncoderPort.matches("demo123", "$2a$10$hash")).thenReturn(true);
        when(tokenProviderPort.createToken("demo")).thenReturn("jwt.token.here");

        StepVerifier.create(loginService.login(new LoginRequest("demo", "demo123")))
                .expectNextMatches(r -> "jwt.token.here".equals(r.getToken()) && "demo".equals(r.getUsername()))
                .verifyComplete();

        verify(loadUserPort).findByUsername("demo");
        verify(tokenProviderPort).createToken("demo");
    }

    @Test
    void shouldReturnEmptyWhenPasswordWrong() {
        User user = User.withId(1L, "demo", "$2a$10$hash");
        when(loadUserPort.findByUsername("demo")).thenReturn(Mono.just(user));
        when(passwordEncoderPort.matches("wrong", "$2a$10$hash")).thenReturn(false);

        StepVerifier.create(loginService.login(new LoginRequest("demo", "wrong")))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenUserNotFound() {
        when(loadUserPort.findByUsername("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(loginService.login(new LoginRequest("unknown", "pass")))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenRequestNull() {
        StepVerifier.create(loginService.login(null))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenUsernameNull() {
        StepVerifier.create(loginService.login(new LoginRequest(null, "pass")))
                .verifyComplete();
    }
}
