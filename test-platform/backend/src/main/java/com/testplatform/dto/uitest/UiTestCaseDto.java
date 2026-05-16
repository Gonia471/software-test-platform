package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UiTestCaseDto {

    private Long id;
    private String name;
    private String description;
    private String moduleKey;
    private String creator;
    private List<Map<String, Object>> steps;
    private Long organizationId;
    private Long projectId;
    private Instant createdAt;
    private Instant updatedAt;
}

