package com.testplatform.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProjectRequest {
    @Size(max = 128, message = "项目名称不能超过128字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500字符")
    private String description;

    @Size(max = 7, message = "颜色格式不正确")
    private String color;

    private String type;
    private String itemsJson;
    private String cronExpression;
    private Integer loopCount;
    private Long uiInstanceId;
    private Boolean enabled;
}
