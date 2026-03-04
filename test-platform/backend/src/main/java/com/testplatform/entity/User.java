package com.testplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String passwordEncrypted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public User() {
    }

    public User(String username, String passwordEncrypted) {
        this.username = username;
        this.passwordEncrypted = passwordEncrypted;
    }
}
