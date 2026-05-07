package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ExecutionInstanceDto {

    private Long id;
    private String name;
    private String type;
    private boolean enabled;
    private Map<String, Object> config;
}

