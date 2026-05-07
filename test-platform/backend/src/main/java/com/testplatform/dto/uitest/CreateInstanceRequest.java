package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateInstanceRequest {

    private String name;
    private String type;
    private String remoteUrl;
    private Map<String, Object> config;
}

