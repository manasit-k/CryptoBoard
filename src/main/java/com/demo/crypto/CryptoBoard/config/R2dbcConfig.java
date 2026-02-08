package com.demo.crypto.CryptoBoard.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * ตั้งค่า R2DBC — สร้าง ConnectionFactory จาก H2 + รัน schema.sql เมื่อเริ่มแอป
 * ใช้แค่ DatabaseClient (non-blocking) ไม่พึ่ง Spring Data Repository
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public ConnectionFactory connectionFactory(
            @Value("${spring.r2dbc.url}") String url,
            @Value("${spring.r2dbc.username:}") String username,
            @Value("${spring.r2dbc.password:}") String password) {
        return ConnectionFactoryBuilder.withUrl(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        initializer.setDatabasePopulator(
                new ResourceDatabasePopulator(new ClassPathResource("schema.sql"))
        );
        return initializer;
    }
}
