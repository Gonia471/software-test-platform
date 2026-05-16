package com.testplatform.entity.apitest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "api_prescript")
public class ApiPrescript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private ApiCollection collection;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 16)
    private StepType stepType = StepType.HTTP;

    @Column(name = "method", length = 16)
    private String method;

    @Column(length = 2000)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String headersJson;

    @Column(columnDefinition = "TEXT")
    private String bodyJson;

    @Column(columnDefinition = "TEXT")
    private String extractParamsJson;

    @Column(name = "function_name", length = 128)
    private String functionName;

    @Column(name = "function_params", columnDefinition = "TEXT")
    private String functionParamsJson;

    @Column(name = "assertions_json", columnDefinition = "TEXT")
    private String assertionsJson;

    @Column(name = "set_variables_json", columnDefinition = "TEXT")
    private String setVariablesJson;

    @Column(name = "stop_on_fail", nullable = false)
    private Boolean stopOnFail = false;

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

    public enum StepType {
        HTTP,
        FUNCTION,
        SET_VARIABLE
    }
}