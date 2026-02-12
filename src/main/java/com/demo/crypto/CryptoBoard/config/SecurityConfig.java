package com.demo.crypto.CryptoBoard.config;

import com.demo.crypto.CryptoBoard.application.port.out.TokenValidatorPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * ตั้งค่า Spring Security WebFlux: เปิดเฉพาะ JWT สำหรับ /api/* (ยกเว้น /api/auth/*)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationConverter jwtAuthConverter,
            ReactiveAuthenticationManager reactiveAuthManager) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(reactiveAuthManager);
        jwtFilter.setServerAuthenticationConverter(jwtAuthConverter);
        jwtFilter.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        jwtFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.anyExchange());

        return http
                .csrf(CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/", "/index.html", "/static/**", "/favicon.ico").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().permitAll())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }))
                .build();
    }

    @Bean
    public ServerAuthenticationConverter jwtAuthenticationConverter(TokenValidatorPort tokenValidator) {
        return exchange -> extractBearerToken(exchange)
                .flatMap(tokenValidator::validateAndGetUsername)
                .map(username -> (Authentication) new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))))
                .switchIfEmpty(Mono.empty());
    }

    private static Mono<String> extractBearerToken(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(h -> h.startsWith(BEARER_PREFIX))
                .map(h -> h.substring(BEARER_PREFIX.length()).trim());
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthManager() {
        return authentication -> Mono.just(authentication);
    }
}
