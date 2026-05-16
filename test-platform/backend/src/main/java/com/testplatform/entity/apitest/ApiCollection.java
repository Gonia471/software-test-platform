package com.testplatform.entity.apitest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "api_collection")
public class ApiCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private NodeType nodeType = NodeType.FOLDER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ApiCollection parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ApiCollection> children = new ArrayList<>();

    @Column(name = "stop_on_fail", nullable = false)
    private Boolean stopOnFail = false;

    @Column(name = "method", length = 10)
    private String method;

    @Column(length = 2000)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String paramsJson;

    @Column(columnDefinition = "TEXT")
    private String headersJson;

    @Column(name = "body_type", length = 20)
    private String bodyType;

    @Column(name = "body_raw", columnDefinition = "TEXT")
    private String bodyRaw;

    @Column(name = "body_raw_type", length = 20)
    private String bodyRawType;

    @Column(name = "body_form", columnDefinition = "TEXT")
    private String bodyForm;

    @Column(name = "auth_type", length = 20)
    private String authType;

    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;

    @Column(columnDefinition = "TEXT")
    private String assertions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private com.testplatform.entity.User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private com.testplatform.entity.Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private com.testplatform.entity.Project project;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum NodeType {
        FOLDER,
        CASE
    }
}
