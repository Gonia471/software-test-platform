package com.testplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "theme_mode", nullable = false, length = 20)
    private String themeMode = "light";

    @Column(name = "notify_on_complete", nullable = false)
    private Boolean notifyOnComplete = true;

    @Column(length = 10)
    private String language = "zh-CN";

    @Column(name = "page_size")
    private Integer pageSize = 20;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UserSettings() {
    }

    public UserSettings(Long userId) {
        this.userId = userId;
        this.themeMode = "light";
        this.notifyOnComplete = true;
        this.language = "zh-CN";
        this.pageSize = 20;
    }
}