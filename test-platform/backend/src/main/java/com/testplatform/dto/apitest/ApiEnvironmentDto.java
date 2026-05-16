package com.testplatform.dto.apitest;

import lombok.Data;

import java.util.List;

@Data
public class ApiEnvironmentDto {

    private Long id;

    private String name;

    private List<VariableDto> variables;

    private List<GlobalVariableDto> globalVariables;

    private java.time.Instant createdAt;

    private java.time.Instant updatedAt;

    @Data
    public static class VariableDto {
        private String key;
        private String value;
        private Boolean enabled;
    }

    @Data
    public static class GlobalVariableDto {
        private String key;
        private String value;
        private String description;
        private Boolean enabled;
    }
}