package com.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotNull(message = "组织ID不能为空")
    private Long organizationId;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128, message = "项目名称不能超过128字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500字符")
    private String description;

    @Size(max = 7, message = "颜色格式不正确")
    private String color;

    private String type; // UI or API
    private String itemsJson;
    private String cronExpression;
    private Integer loopCount;
    private Long uiInstanceId;
    private Boolean enabled;
}
