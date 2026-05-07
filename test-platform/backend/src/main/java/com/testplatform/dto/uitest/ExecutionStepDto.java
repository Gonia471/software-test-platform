package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ExecutionStepDto {

    private Integer index;
    private String stepType;
    private String action;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private String errorMessage;
    private String screenshotUrl;
    private String logText;
}

