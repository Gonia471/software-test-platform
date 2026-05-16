package com.testplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRequest {
    private String role;
    private Boolean canRead;
    private Boolean canWrite;
}