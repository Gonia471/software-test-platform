package com.testplatform.service.uitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.uitest.CreateOrUpdateCaseRequest;
import com.testplatform.dto.uitest.UiTestCaseDto;
import com.testplatform.entity.uitest.UiTestCase;
import com.testplatform.repository.uitest.UiTestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UiTestCaseService {

    private final UiTestCaseRepository testCaseRepository;
    private final ObjectMapper objectMapper;

    public UiTestCaseService(UiTestCaseRepository testCaseRepository, ObjectMapper objectMapper) {
        this.testCaseRepository = testCaseRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UiTestCaseDto create(CreateOrUpdateCaseRequest req) {
        validateRequest(req, true);
        UiTestCase entity = new UiTestCase();
        entity.setName(req.getName() != null && !req.getName().isBlank() ? req.getName() : "未命名用例");
        entity.setDescription(req.getDescription());
        entity.setStepsJson(writeSteps(req.getSteps()));
        UiTestCase saved = testCaseRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public UiTestCaseDto update(Long id, CreateOrUpdateCaseRequest req) {
        validateRequest(req, false);
        UiTestCase entity = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("测试用例不存在: " + id));
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getSteps() != null) entity.setStepsJson(writeSteps(req.getSteps()));
        UiTestCase saved = testCaseRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<UiTestCaseDto> listAll() {
        return testCaseRepository.findAll().stream()
                .map(this::toDtoWithoutSteps)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!testCaseRepository.existsById(id)) {
            throw new IllegalArgumentException("测试用例不存在: " + id);
        }
        testCaseRepository.deleteById(id);
    }

    private String writeSteps(List<Map<String, Object>> steps) {
        try {
            return objectMapper.writeValueAsString(steps != null ? steps : List.of());
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化步骤失败", e);
        }
    }

    private UiTestCaseDto toDto(UiTestCase entity) {
        UiTestCaseDto dto = new UiTestCaseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setSteps(readSteps(entity.getStepsJson()));
        return dto;
    }

    /** 用于列表场景，不反序列化 steps，减小 payload */
    private UiTestCaseDto toDtoWithoutSteps(UiTestCase entity) {
        UiTestCaseDto dto = new UiTestCaseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    private void validateRequest(CreateOrUpdateCaseRequest req, boolean creating) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (creating) {
            if (req.getName() == null || req.getName().isBlank()) {
                throw new IllegalArgumentException("用例名称不能为空");
            }
            if (req.getSteps() == null || req.getSteps().isEmpty()) {
                throw new IllegalArgumentException("步骤列表不能为空");
            }
        }
    }

    @Transactional(readOnly = true)
    public UiTestCaseDto getDetail(Long id) {
        UiTestCase entity = testCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("测试用例不存在: " + id));
        return toDto(entity);
    }

    private List<Map<String, Object>> readSteps(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("解析用例步骤失败", e);
        }
    }
}

