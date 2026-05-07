package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ExecutionDetailDto {

    private Long id;
    private Long testCaseId;
    private Long instanceId;
    private String status;
    private Map<String, Object> options;
    private Instant startTime;
    private Instant endTime;
    private String errorMessage;
    private List<ExecutionStepDto> steps;
}

