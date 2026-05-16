package com.testplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSettingsDto {
    private Long userId;
    private String themeMode;
    private Boolean notifyOnComplete;
    private String language;
    private Integer pageSize;
}