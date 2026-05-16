package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.apitest.ApiEnvironmentDto;
import com.testplatform.entity.User;
import com.testplatform.entity.apitest.ApiEnvironment;
import com.testplatform.repository.apitest.ApiEnvironmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiEnvironmentService {

    private final ApiEnvironmentRepository environmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiEnvironmentDto> getEnvironments(Long userId) {
        return environmentRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiEnvironmentDto create(ApiEnvironmentDto dto, User user) {
        ApiEnvironment environment = toEntity(dto);
        environment.setUser(user);
        ApiEnvironment saved = environmentRepository.save(environment);
        return toDto(saved);
    }

    @Transactional
    public ApiEnvironmentDto update(Long id, ApiEnvironmentDto dto, User user) {
        ApiEnvironment environment = environmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("环境不存在: " + id));

        if (!environment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("无权限修改该环境");
        }

        environment.setName(dto.getName());
        environment.setVariablesJson(toJson(dto.getVariables()));

        ApiEnvironment saved = environmentRepository.save(environment);
        return toDto(saved);
    }

    @Transactional
    public void delete(Long id, User user) {
        if (!environmentRepository.existsByIdAndUserId(id, user.getId())) {
            throw new IllegalArgumentException("环境不存在或无权限删除");
        }
        environmentRepository.deleteById(id);
    }

    private ApiEnvironmentDto toDto(ApiEnvironment entity) {
        ApiEnvironmentDto dto = new ApiEnvironmentDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
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
