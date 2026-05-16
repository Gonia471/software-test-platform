package com.testplatform.service;

import com.testplatform.dto.OrgConfigDto;
import com.testplatform.entity.OrgConfig;
import com.testplatform.repository.OrgConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;

    public OrgConfigDto getConfig(Long organizationId) {
        OrgConfig config = orgConfigRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultConfig(organizationId));
        return convertToDto(config);
    }

    @Transactional
    public OrgConfigDto updateConfig(Long organizationId, OrgConfigDto dto) {
        OrgConfig config = orgConfigRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> createDefaultConfig(organizationId));

        if (dto.getDefaultBrowser() != null) {
            config.setDefaultBrowser(dto.getDefaultBrowser());
        }
        if (dto.getDriverMode() != null) {
            config.setDriverMode(dto.getDriverMode());
        }
        if (dto.getRemoteUrl() != null) {
            config.setRemoteUrl(dto.getRemoteUrl());
        }
        if (dto.getStepTimeout() != null) {
            config.setStepTimeout(dto.getStepTimeout());
        }
        if (dto.getScreenshotStrategy() != null) {
            config.setScreenshotStrategy(dto.getScreenshotStrategy());
        }
        if (dto.getWindowSize() != null) {
            config.setWindowSize(dto.getWindowSize());
        }
        if (dto.getApiTimeout() != null) {
            config.setApiTimeout(dto.getApiTimeout());
        }
        if (dto.getProxyServer() != null) {
            config.setProxyServer(dto.getProxyServer());
        }
        if (dto.getSslVerify() != null) {
            config.setSslVerify(dto.getSslVerify());
        }
        if (dto.getRetryCount() != null) {
            config.setRetryCount(dto.getRetryCount());
        }

        return convertToDto(orgConfigRepository.save(config));
    }

    private OrgConfig createDefaultConfig(Long organizationId) {
        OrgConfig config = new OrgConfig(organizationId);
        return orgConfigRepository.save(config);
    }

    private OrgConfigDto convertToDto(OrgConfig config) {
        OrgConfigDto dto = new OrgConfigDto();
        dto.setOrganizationId(config.getOrganizationId());
        dto.setDefaultBrowser(config.getDefaultBrowser());
        dto.setDriverMode(config.getDriverMode());
        dto.setRemoteUrl(config.getRemoteUrl());
        dto.setStepTimeout(config.getStepTimeout());
        dto.setScreenshotStrategy(config.getScreenshotStrategy());
        dto.setWindowSize(config.getWindowSize());
        dto.setApiTimeout(config.getApiTimeout());
        dto.setProxyServer(config.getProxyServer());
        dto.setSslVerify(config.getSslVerify());
        dto.setRetryCount(config.getRetryCount());
        return dto;
    }
}