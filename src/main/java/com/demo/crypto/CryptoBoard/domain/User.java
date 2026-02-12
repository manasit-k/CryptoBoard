package com.demo.crypto.CryptoBoard.domain;

/**
 * Domain Entity สำหรับผู้ใช้ (ใช้กับ Login/Register)
 * เก็บเฉพาะข้อมูลที่จำเป็น — ไม่พึ่ง framework
 */
public class User {

    private final Long id;
    private final String username;
    private final String passwordHash;

    private User(Long id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public static User withId(Long id, String username, String passwordHash) {
        return new User(id, username, passwordHash);
    }

    public static User withoutId(String username, String passwordHash) {
        return new User(null, username, passwordHash);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
