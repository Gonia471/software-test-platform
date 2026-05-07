package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class UiTestCaseDto {

    private Long id;
    private String name;
    private String description;
    private List<Map<String, Object>> steps;
}

