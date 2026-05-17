package com.testplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddOrganizationMemberRequest {

    @NotNull(message = "成员不能为空")
    private Long userId;

    private String role = "MEMBER";

    private Boolean canRead = true;

    private Boolean canWrite = false;
}
