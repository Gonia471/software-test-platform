package com.testplatform.dto.apitest;

import com.testplatform.entity.apitest.ApiCollection;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class ApiCollectionDto {

    private Long id;

    private String name;

    private String description;

    private ApiCollection.NodeType nodeType;

    private String type; // 前端使用的别名

    private Long parentId;

    private String method;

    private String url;

    private List<Map<String, Object>> params;

    private List<Map<String, Object>> headers;

    private String bodyType;

    private String bodyRaw;

    private String bodyRawType;

    private List<Map<String, Object>> bodyForm;

    private String authType;

    private Map<String, Object> authConfig;

    private List<Map<String, Object>> assertions;

    private List<ApiCollectionDto> children;

    private Long organizationId;

    private Long projectId;

    private String creatorUsername;

    private Instant createdAt;

    private Instant updatedAt;
}
