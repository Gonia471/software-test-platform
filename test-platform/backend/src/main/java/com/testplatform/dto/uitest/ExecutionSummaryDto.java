package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ExecutionSummaryDto {

    private Long id;
    private Long testCaseId;
    private String testCaseName;
    private Long projectId;
    private String projectName;
    private Long instanceId;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private Instant createdAt;
}
