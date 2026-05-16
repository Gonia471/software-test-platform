package com.testplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(length = 6)
    private String verificationCode;

    @Column
    private Instant verificationCodeSentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Boolean isDevMode = false;

    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public boolean isDevMode() {
        return Boolean.TRUE.equals(this.isDevMode);
    }
}