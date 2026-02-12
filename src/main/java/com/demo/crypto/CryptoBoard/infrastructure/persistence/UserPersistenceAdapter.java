package com.demo.crypto.CryptoBoard.infrastructure.persistence;

import com.demo.crypto.CryptoBoard.application.port.out.LoadUserPort;
import com.demo.crypto.CryptoBoard.application.port.out.SaveUserPort;
import com.demo.crypto.CryptoBoard.domain.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Adapter: เก็บ/โหลด User ผ่าน JDBC (แบบเดียวกับ PriceHistory — ใช้ boundedElastic)
 */
@Component
public class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

    private final JdbcTemplate jdbc;

    public UserPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return Mono.fromCallable(() -> {
            List<User> list = jdbc.query(
                    "SELECT id, username, password_hash FROM app_user WHERE username = ?",
                    (rs, rowNum) -> User.withId(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash")
                    ),
                    username
            );
            return list.isEmpty() ? null : list.get(0);
        })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(u -> u == null ? Mono.empty() : Mono.just(u));
    }

    @Override
    public Mono<User> save(User user) {
        return Mono.fromCallable(() -> {
            var keyHolder = new GeneratedKeyHolder();
            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO app_user (username, password_hash, created_at) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPasswordHash());
                ps.setTimestamp(3, Timestamp.from(Instant.now()));
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new IllegalStateException("Failed to get generated id");
            }
            return User.withId(key.longValue(), user.getUsername(), user.getPasswordHash());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
