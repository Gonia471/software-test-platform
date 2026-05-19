package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.apitest.ApiEnvironmentDto;
import com.testplatform.entity.Organization;
import com.testplatform.entity.User;
import com.testplatform.entity.apitest.ApiEnvironment;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.apitest.ApiEnvironmentRepository;
import com.testplatform.service.OrganizationPermissionService;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiEnvironmentService {

    private final ApiEnvironmentRepository environmentRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationPermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiEnvironmentDto> getEnvironments(User user) {
        Map<Long, ApiEnvironment> merged = new LinkedHashMap<>();
        List<Long> accessibleOrgIds = permissionService.getAccessibleOrganizationIds(user);

        if (!accessibleOrgIds.isEmpty()) {
            environmentRepository.findByOrganizationIdInOrderByIdAsc(accessibleOrgIds)
                    .forEach(environment -> merged.put(environment.getId(), environment));
        }

        environmentRepository.findByUserIdAndOrganizationIsNullOrderByIdAsc(user.getId())
                .forEach(environment -> merged.put(environment.getId(), environment));

        return new ArrayList<>(merged.values()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiEnvironmentDto create(ApiEnvironmentDto dto, User user) {
        ApiEnvironment environment = toEntity(dto);
        environment.setUser(user);

        Organization organization = resolveOrganization(dto.getOrganizationId(), user, false);
        environment.setOrganization(organization);

        ApiEnvironment saved = environmentRepository.save(environment);
        return toDto(saved);
    }

    @Transactional
    public ApiEnvironmentDto update(Long id, ApiEnvironmentDto dto, User user) {
        ApiEnvironment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("环境不存在: " + id));

        validateManagePermission(environment, user);

        environment.setName(dto.getName());
        environment.setVariablesJson(toJson(dto.getVariables()));
        environment.setGlobalVariablesJson(toJson(dto.getGlobalVariables()));

        ApiEnvironment saved = environmentRepository.save(environment);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id, User user) {
        ApiEnvironment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("环境不存在: " + id));

        validateManagePermission(environment, user);
        environmentRepository.delete(environment);
    }

    private ApiEnvironmentDto toDto(ApiEnvironment entity) {
        ApiEnvironmentDto dto = new ApiEnvironmentDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setOrganizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null);
        dto.setVariables(parseJson(entity.getVariablesJson(), new TypeReference<>() {}));
        dto.setGlobalVariables(parseJson(entity.getGlobalVariablesJson(), new TypeReference<>() {}));
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private ApiEnvironment toEntity(ApiEnvironmentDto dto) {
        ApiEnvironment environment = new ApiEnvironment();
        environment.setName(dto.getName());
        environment.setVariablesJson(toJson(dto.getVariables()));
        environment.setGlobalVariablesJson(toJson(dto.getGlobalVariables()));
        return environment;
    }

    private Organization resolveOrganization(Long organizationId, User user, boolean requireManagePermission) {
        if (organizationId == null) {
            throw new IllegalArgumentException("请选择所属组织");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

        boolean permitted = requireManagePermission
                ? permissionService.canManageOrganization(organizationId, user)
                : permissionService.canViewOrganization(organizationId, user);
        if (!permitted) {
            throw new IllegalArgumentException("无权限访问该组织环境");
        }
        return organization;
    }

    private void validateManagePermission(ApiEnvironment environment, User user) {
        if (SecurityUtils.isDevMode()) {
            return;
        }
        if (environment.getOrganization() != null) {
            if (!permissionService.canManageOrganization(environment.getOrganization().getId(), user)) {
                throw new IllegalArgumentException("无权限修改该环境");
            }
            return;
        }

        if (!environment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("无权限修改该环境");
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON解析失败: " + json, e);
        }
    }
}
