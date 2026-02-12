package com.demo.crypto.CryptoBoard.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO สำหรับ request ลงทะเบียนผู้ใช้ใหม่
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {

    private String username;
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
