package com.testplatform.entity.uitest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ui_execution_step")
public class UiExecutionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Column(name = "step_type", length = 32)
    private String stepType;

    @Column(name = "action", length = 64)
    private String action;

    @Column(nullable = false, length = 32)
    private String status; // PENDING/RUNNING/PASSED/FAILED/SKIPPED

    private Instant startTime;

    private Instant endTime;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "screenshot_path", length = 255)
    private String screenshotPath;

    @Lob
    @Column(name = "log_text")
    private String logText;

    @Lob
    @Column(name = "raw_step_json")
    private String rawStepJson;
}

