package com.demo.crypto.CryptoBoard.interfaces.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * แปลง exception เป็น HTTP status + body
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthController.UnauthorizedException.class)
    public Mono<ResponseEntity<Map<String, String>>> unauthorized(AuthController.UnauthorizedException ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", ex.getMessage())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, String>>> badRequest(IllegalArgumentException ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage())));
    }
}
