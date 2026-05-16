package com.testplatform.dto.apitest;

import lombok.Data;

import java.time.Instant;

@Data
public class ApiTestExecutionSummaryDto {
    private Long id;
    private String collectionId;
    private String collectionName;
    private Long projectId;
    private String projectName;
    private String status;
    private Integer duration;
    private Integer httpStatus;
    private String statusText;
    private Instant createdAt;
}