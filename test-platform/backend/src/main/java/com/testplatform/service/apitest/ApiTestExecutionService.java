package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.apitest.ApiTestExecutionDetailDto;
import com.testplatform.dto.apitest.ApiTestExecutionSummaryDto;
import com.testplatform.dto.apitest.SaveExecutionRequest;
import com.testplatform.entity.User;
import com.testplatform.entity.apitest.ApiTestExecution;
import com.testplatform.repository.apitest.ApiTestExecutionRepository;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiTestExecutionService {

    private final ApiTestExecutionRepository executionRepository;
    private final com.testplatform.repository.ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiTestExecutionSummaryDto> listRecentExecutions(Long userId, int limit) {
        List<ApiTestExecution> executions;
        if (SecurityUtils.isDevMode()) {
            executions = executionRepository.findAll(PageRequest.of(0, limit)).getContent();
        } else {
            executions = executionRepository.findRecentByUserId(
                    userId, PageRequest.of(0, limit));
        }

        return executions.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApiTestExecutionDetailDto getDetail(Long id, Long userId) {
        ApiTestExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("执行记录不存在: " + id));

        if (!SecurityUtils.isDevMode() && !execution.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("无权限访问该执行记录");
        }

        return toDetailDto(execution);
    }

    @Transactional
    public ApiTestExecutionSummaryDto saveExecution(SaveExecutionRequest request, User user) {
        ApiTestExecution execution = new ApiTestExecution();
        execution.setUser(user);
        execution.setCollectionId(request.getCollectionId());
        execution.setCollectionName(request.getCollectionName());
        execution.setStatus(ApiTestExecution.TestStatus.valueOf(request.getStatus()));
        execution.setErrorMessage(request.getErrorMessage());

        if (request.getRequest() != null) {
            execution.setRequestJson(toJson(request.getRequest()));
            if (request.getRequest().getHeaders() != null) {
                execution.setRequestHeaders(toJson(request.getRequest().getHeaders()));
            }
            execution.setRequestBody(request.getRequest().getBody());
        }

        if (request.getResponse() != null) {
            execution.setResponseJson(toJson(request.getResponse()));
            execution.setHttpStatus(request.getResponse().getStatus());
            execution.setStatusText(request.getResponse().getStatusText());
            execution.setDuration(request.getResponse().getDuration());
            if (request.getResponse().getHeaders() != null) {
                execution.setResponseHeaders(toJson(request.getResponse().getHeaders()));
            }
            execution.setResponseBody(request.getResponse().getBody());
        }

        if (request.getAssertions() != null) {
            execution.setAssertionsJson(toJson(request.getAssertions()));
        }

        if (request.getPrescriptResults() != null) {
            execution.setPrescriptResultsJson(toJson(request.getPrescriptResults()));
        }

        ApiTestExecution saved = executionRepository.save(execution);
        return toSummaryDto(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics(Long userId) {
        long total;
        long success;
        long failed;
        long error;
        if (SecurityUtils.isDevMode()) {
            List<ApiTestExecution> executions = executionRepository.findAll();
            total = executions.size();
            success = executions.stream().filter(item -> item.getStatus() == ApiTestExecution.TestStatus.SUCCESS).count();
            failed = executions.stream().filter(item -> item.getStatus() == ApiTestExecution.TestStatus.FAILED).count();
            error = executions.stream().filter(item -> item.getStatus() == ApiTestExecution.TestStatus.ERROR).count();
        } else {
            total = executionRepository.countByUserId(userId);
            success = executionRepository.countByUserIdAndStatus(userId, ApiTestExecution.TestStatus.SUCCESS);
            failed = executionRepository.countByUserIdAndStatus(userId, ApiTestExecution.TestStatus.FAILED);
            error = executionRepository.countByUserIdAndStatus(userId, ApiTestExecution.TestStatus.ERROR);
        }

        return Map.of(
                "total", total,
                "success", success,
                "failed", failed,
                "error", error,
                "successRate", total > 0 ? (double) success / total * 100 : 0
        );
    }

    private ApiTestExecutionSummaryDto toSummaryDto(ApiTestExecution entity) {
        ApiTestExecutionSummaryDto dto = new ApiTestExecutionSummaryDto();
        dto.setId(entity.getId());
        dto.setCollectionId(entity.getCollectionId());
        dto.setCollectionName(entity.getCollectionName());
        dto.setProjectId(entity.getProjectId());
        dto.setStatus(entity.getStatus().name());
        dto.setDuration(entity.getDuration());
        dto.setHttpStatus(entity.getHttpStatus());
        dto.setStatusText(entity.getStatusText());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getProjectId() != null) {
            projectRepository.findById(entity.getProjectId()).ifPresent(p -> dto.setProjectName(p.getName()));
        }

        return dto;
    }

    private ApiTestExecutionDetailDto toDetailDto(ApiTestExecution entity) {
        ApiTestExecutionDetailDto dto = new ApiTestExecutionDetailDto();
        dto.setId(entity.getId());
        dto.setCollectionId(entity.getCollectionId());
        dto.setCollectionName(entity.getCollectionName());
        dto.setProjectId(entity.getProjectId());
        dto.setStatus(entity.getStatus().name());
        dto.setDuration(entity.getDuration());
        dto.setHttpStatus(entity.getHttpStatus());
        dto.setStatusText(entity.getStatusText());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getProjectId() != null) {
            projectRepository.findById(entity.getProjectId()).ifPresent(project -> dto.setProjectName(project.getName()));
        }

        dto.setRequest(parseJson(entity.getRequestJson(), ApiTestExecutionDetailDto.RequestDto.class));
        dto.setResponse(parseJson(entity.getResponseJson(), ApiTestExecutionDetailDto.ResponseDto.class));
        dto.setAssertions(parseJsonList(entity.getAssertionsJson()));
        dto.setPrescriptResults(parseJsonList(entity.getPrescriptResultsJson()));

        return dto;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private <T> List<T> parseJsonList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
