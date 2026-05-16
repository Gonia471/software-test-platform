package com.testplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "org_config")
public class OrgConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    @Column(name = "default_browser", nullable = false, length = 20)
    private String defaultBrowser = "chrome";

    @Column(name = "driver_mode", nullable = false, length = 20)
    private String driverMode = "local";

    @Column(name = "remote_url")
    private String remoteUrl;

    @Column(name = "step_timeout", nullable = false)
    private Integer stepTimeout = 30;

    @Column(name = "screenshot_strategy", nullable = false, length = 20)
    private String screenshotStrategy = "on_failure";

    @Column(name = "window_size", nullable = false, length = 20)
    private String windowSize = "1920x1080";

    @Column(name = "api_timeout", nullable = false)
    private Integer apiTimeout = 30000;

    @Column(name = "proxy_server")
    private String proxyServer;

    @Column(name = "ssl_verify", nullable = false)
    private Boolean sslVerify = true;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public OrgConfig() {
    }

    public OrgConfig(Long organizationId) {
        this.organizationId = organizationId;
        this.defaultBrowser = "chrome";
        this.driverMode = "local";
        this.stepTimeout = 30;
        this.screenshotStrategy = "on_failure";
        this.windowSize = "1920x1080";
        this.apiTimeout = 30000;
        this.sslVerify = true;
        this.retryCount = 0;
    }
}