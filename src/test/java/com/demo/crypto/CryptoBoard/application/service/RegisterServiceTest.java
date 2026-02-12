package com.demo.crypto.CryptoBoard.application.service;

import com.demo.crypto.CryptoBoard.application.dto.RegisterRequest;
import com.demo.crypto.CryptoBoard.application.port.out.LoadUserPort;
import com.demo.crypto.CryptoBoard.application.port.out.PasswordEncoderPort;
import com.demo.crypto.CryptoBoard.application.port.out.SaveUserPort;
import com.demo.crypto.CryptoBoard.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private LoadUserPort loadUserPort;
    @Mock
    private PasswordEncoderPort passwordEncoderPort;
    @Mock
    private SaveUserPort saveUserPort;

    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(loadUserPort, passwordEncoderPort, saveUserPort);
    }

    @Test
    void shouldSaveUserWhenUsernameAvailable() {
        when(loadUserPort.findByUsername("newuser")).thenReturn(Mono.empty());
        when(passwordEncoderPort.encode("pass1234")).thenReturn("$2a$10$hashed");
        when(saveUserPort.save(any(User.class))).thenReturn(Mono.just(User.withId(1L, "newuser", "hash")));

        StepVerifier.create(registerService.register(new RegisterRequest("newuser", "pass1234")))
                .verifyComplete();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(captor.capture());
        assertEquals("newuser", captor.getValue().getUsername());
        assertEquals("$2a$10$hashed", captor.getValue().getPasswordHash());
    }

    @Test
    void shouldErrorWhenUsernameTaken() {
        when(loadUserPort.findByUsername("existing")).thenReturn(Mono.just(User.withId(1L, "existing", "hash")));

        StepVerifier.create(registerService.register(new RegisterRequest("existing", "pass1234")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldErrorWhenPasswordTooShort() {
        StepVerifier.create(registerService.register(new RegisterRequest("user", "123")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldErrorWhenRequestNull() {
        StepVerifier.create(registerService.register(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldErrorWhenUsernameEmptyAfterTrim() {
        StepVerifier.create(registerService.register(new RegisterRequest("   ", "pass1234")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
