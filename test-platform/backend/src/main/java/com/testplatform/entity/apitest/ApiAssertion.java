package com.testplatform.entity.apitest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "api_assertion")
public class ApiAssertion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private ApiCollection collection;

    @Enumerated(EnumType.STRING)
    @Column(name = "assertion_type", nullable = false, length = 32)
    private AssertionType assertionType = AssertionType.STATUS;

    @Column(length = 512)
    private String expression;

    @Column(length = 256)
    private String expected;

    @Column
    private Boolean enabled = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum AssertionType {
        STATUS,
        JSONPATH,
        CONTAINS,
        DURATION,
        HEADERS,
        FUNCTION
    }
}