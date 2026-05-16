package com.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank(message = "组织名称不能为空")
    @Size(max = 128, message = "组织名称不能超过128字符")
    private String name;

    @Size(max = 500, message = "描述不能超过500字符")
    private String description;

    @Size(max = 7, message = "颜色格式不正确")
    private String color;
}