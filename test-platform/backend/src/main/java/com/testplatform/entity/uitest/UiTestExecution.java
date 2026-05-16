package com.testplatform.entity.uitest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ui_test_execution")
public class UiTestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_case_id", nullable = false)
    private Long testCaseId;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(nullable = false, length = 32)
    private String status; // PENDING/RUNNING/PASSED/FAILED/STOPPED

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    private Instant startTime;

    private Instant endTime;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stop_requested", nullable = false)
    private Boolean stopRequested = false;

    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

