package com.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32)
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 64)
    private String password;
}
