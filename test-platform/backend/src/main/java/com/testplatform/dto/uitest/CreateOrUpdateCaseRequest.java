package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CreateOrUpdateCaseRequest {

    private String name;
    private String description;
    private String moduleKey;
    private List<Map<String, Object>> steps;
    private Long organizationId;
    private Long projectId;
}
