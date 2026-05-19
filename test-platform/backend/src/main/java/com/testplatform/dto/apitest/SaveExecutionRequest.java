package com.testplatform.dto.apitest;

import lombok.Data;

@Data
public class SaveExecutionRequest {
    private String collectionId;
    private String collectionName;
    private String status;
    private String errorMessage;
    private RequestDto request;
    private ResponseDto response;
    private java.util.List<AssertionDto> assertions;
    private java.util.List<PrescriptResultDto> prescriptResults;

    @Data
    public static class RequestDto {
        private String method;
        private String url;
        private java.util.Map<String, String> headers;
        private String body;
    }

    @Data
    public static class ResponseDto {
        private Integer status;
        private String statusText;
        private java.util.Map<String, String> headers;
        private String body;
        private Integer size;
        private Integer duration;
    }

    @Data
    public static class AssertionDto {
        private String type;
        private String description;
        private Boolean passed;
        private String expected;
        private String actual;
    }

    @Data
    public static class PrescriptResultDto {
        private Integer index;
        private String stepType;
        private String title;
        private String status;
        private Boolean stopOnFail;
        private String message;
        private String errorMessage;
        private String variableName;
        private String variableValue;
        private RequestDto request;
        private ResponseDto response;
        private java.util.List<AssertionDto> assertions;
        private java.util.List<ExtractedVariableDto> extractedVariables;
    }

    @Data
    public static class ExtractedVariableDto {
        private String name;
        private String path;
        private String value;
    }
}
