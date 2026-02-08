package com.demo.crypto.CryptoBoard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcInitializationAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = R2dbcInitializationAutoConfiguration.class)
@EnableScheduling
public class CryptoBoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoBoardApplication.class, args);
	}

}
