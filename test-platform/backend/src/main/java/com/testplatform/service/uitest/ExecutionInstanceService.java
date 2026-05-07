package com.testplatform.service.uitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.uitest.CreateInstanceRequest;
import com.testplatform.dto.uitest.ExecutionInstanceDto;
import com.testplatform.entity.uitest.UiExecutionInstance;
import com.testplatform.repository.uitest.UiExecutionInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExecutionInstanceService {

    private final UiExecutionInstanceRepository instanceRepository;
    private final ObjectMapper objectMapper;

    public ExecutionInstanceService(UiExecutionInstanceRepository instanceRepository, ObjectMapper objectMapper) {
        this.instanceRepository = instanceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<ExecutionInstanceDto> listAll() {
        List<UiExecutionInstance> entities = instanceRepository.findAll();
        if (entities.isEmpty()) {
            UiExecutionInstance local = new UiExecutionInstance();
            local.setName("本地浏览器");
            local.setType("LOCAL");
            local.setEnabled(true);
            local.setConfigJson("{\"browser\":\"chrome\",\"headlessSupported\":true}");
            instanceRepository.save(local);
            entities = List.of(local);
        }
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ExecutionInstanceDto create(CreateInstanceRequest req) {
        UiExecutionInstance entity = new UiExecutionInstance();
        entity.setName(req.getName());
        entity.setType(req.getType() == null ? "REMOTE" : req.getType());
        entity.setRemoteUrl(req.getRemoteUrl());
        entity.setEnabled(true);
        entity.setConfigJson(writeJson(req.getConfig()));
        UiExecutionInstance saved = instanceRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public UiExecutionInstance loadById(Long id) {
        return instanceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("执行实例不存在: " + id));
    }

    private ExecutionInstanceDto toDto(UiExecutionInstance entity) {
        ExecutionInstanceDto dto = new ExecutionInstanceDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        dto.setConfig(readJson(entity.getConfigJson()));
        return dto;
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null;
        }
    }
}

