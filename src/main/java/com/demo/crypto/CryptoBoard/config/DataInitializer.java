package com.demo.crypto.CryptoBoard.config;

import com.demo.crypto.CryptoBoard.application.port.out.LoadUserPort;
import com.demo.crypto.CryptoBoard.application.port.out.PasswordEncoderPort;
import com.demo.crypto.CryptoBoard.application.port.out.SaveUserPort;
import com.demo.crypto.CryptoBoard.domain.User;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * สร้างผู้ใช้ทดสอบ (demo / demo123) ถ้ายังไม่มี — ใช้สำหรับทดสอบ Login
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final String DEMO_USER = "demo";
    private static final String DEMO_PASSWORD = "demo123";

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public DataInitializer(LoadUserPort loadUserPort, SaveUserPort saveUserPort, PasswordEncoderPort passwordEncoderPort) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public void run(ApplicationArguments args) {
        loadUserPort.findByUsername(DEMO_USER)
                .hasElement()
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? reactor.core.publisher.Mono.empty()
                        : saveUserPort.save(User.withoutId(DEMO_USER, passwordEncoderPort.encode(DEMO_PASSWORD))).then())
                .subscribe();
    }
}
