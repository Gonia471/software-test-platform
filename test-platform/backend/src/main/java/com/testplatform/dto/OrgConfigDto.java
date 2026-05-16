package com.testplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgConfigDto {
    private Long organizationId;
    private String defaultBrowser;
    private String driverMode;
    private String remoteUrl;
    private Integer stepTimeout;
    private String screenshotStrategy;
    private String windowSize;
    private Integer apiTimeout;
    private String proxyServer;
    private Boolean sslVerify;
    private Integer retryCount;
}