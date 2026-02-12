package com.demo.crypto.CryptoBoard.infrastructure.security;

import com.demo.crypto.CryptoBoard.application.port.out.TokenProviderPort;
import com.demo.crypto.CryptoBoard.application.port.out.TokenValidatorPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Adapter: สร้างและตรวจสอบ JWT ด้วย library jjwt (implement TokenProviderPort + TokenValidatorPort)
 */
@Component
public class JwtAdapter implements TokenProviderPort, TokenValidatorPort {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public String createToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Mono<String> validateAndGetUsername(String token) {
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            return claims.getSubject();
        })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.empty());
    }
}
