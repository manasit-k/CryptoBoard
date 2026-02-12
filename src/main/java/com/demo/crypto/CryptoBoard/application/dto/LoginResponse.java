package com.demo.crypto.CryptoBoard.application.dto;

/**
 * DTO สำหรับ response หลังล็อกอินสำเร็จ (ส่ง JWT กลับไป)
 */
public class LoginResponse {

    private final String token;
    private final String type = "Bearer";
    private final String username;

    public LoginResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }
}
