package com.testplatform.service.uitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.testplatform.dto.uitest.ExecutionDetailDto;
import com.testplatform.dto.uitest.ExecutionStepDto;
import com.testplatform.dto.uitest.ExecutionSummaryDto;
import com.testplatform.dto.uitest.StartExecutionRequest;
import com.testplatform.dto.uitest.StartExecutionResponse;
import com.testplatform.entity.uitest.UiExecutionInstance;
import com.testplatform.entity.uitest.UiExecutionStep;
import com.testplatform.entity.uitest.UiTestCase;
import com.testplatform.entity.uitest.UiTestExecution;
import com.testplatform.repository.uitest.UiExecutionStepRepository;
import com.testplatform.repository.uitest.UiTestCaseRepository;
import com.testplatform.repository.uitest.UiTestExecutionRepository;
import com.testplatform.service.uitest.model.ExecutionOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UiTestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(UiTestExecutionService.class);

    private final UiTestExecutionRepository executionRepository;
    private final UiExecutionStepRepository stepRepository;
    private final UiTestCaseRepository testCaseRepository;
    private final ExecutionInstanceService instanceService;
    private final UiTestExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    public UiTestExecutionService(UiTestExecutionRepository executionRepository,
                                  UiExecutionStepRepository stepRepository,
                                  UiTestCaseRepository testCaseRepository,
                                  ExecutionInstanceService instanceService,
                                  UiTestExecutionEngine executionEngine,
                                  ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.stepRepository = stepRepository;
        this.testCaseRepository = testCaseRepository;
        this.instanceService = instanceService;
        this.executionEngine = executionEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StartExecutionResponse startExecution(StartExecutionRequest req) {
        // 校验执行实例（主要是为了预留远程实例扩展）
        UiExecutionInstance instance = instanceService.loadById(req.getInstanceId());
        if (!Boolean.TRUE.equals(instance.getEnabled())) {
            throw new IllegalArgumentException("执行实例未启用: " + instance.getName());
        }

        ExecutionOptions options = new ExecutionOptions();
        options.setHeadless(req.isHeadless());
        options.setStopOnFailure(req.isStopOnFailure());
        options.setScreenshotOnFailure(req.isScreenshotOnFailure());
        options.setScreenshotEveryStep(
                req.getScreenshotEveryStep() == null || Boolean.TRUE.equals(req.getScreenshotEveryStep()));

        UiTestExecution execution = new UiTestExecution();
        execution.setTestCaseId(req.getTestCaseId());
        execution.setInstanceId(req.getInstanceId());
        execution.setStatus("PENDING");
        execution.setOptionsJson(writeOptions(options));
        execution.setCreatedAt(Instant.now());
        execution.setUpdatedAt(Instant.now());

        UiTestExecution saved = executionRepository.save(execution);

        // 在当前事务提交后再触发异步执行，避免异步线程读不到未提交的执行记录
        Long executionId = saved.getId();
        log.info("[UI执行] 注册 afterCommit 回调, executionId={}", executionId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("[UI执行] afterCommit 触发，准备启动异步执行, executionId={}", executionId);
                executionEngine.runExecution(executionId, options);
                log.info("[UI执行] 异步任务已提交到线程池, executionId={}", executionId);
            }
        });

        StartExecutionResponse res = new StartExecutionResponse();
        res.setExecutionId(saved.getId());
        res.setStatus(saved.getStatus());
        return res;
    }

    @Transactional(readOnly = true)
    public ExecutionDetailDto getDetail(Long id) {
        UiTestExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("执行记录不存在: " + id));
        ExecutionDetailDto dto = new ExecutionDetailDto();
        dto.setId(execution.getId());
        dto.setTestCaseId(execution.getTestCaseId());
        dto.setInstanceId(execution.getInstanceId());
        dto.setStatus(execution.getStatus());
        dto.setOptions(readOptions(execution.getOptionsJson()));
        dto.setStartTime(execution.getStartTime());
        dto.setEndTime(execution.getEndTime());
        dto.setErrorMessage(execution.getErrorMessage());

        List<UiExecutionStep> steps = stepRepository.findByExecutionIdOrderByStepIndexAsc(id);
        List<ExecutionStepDto> stepDtos = steps.stream()
                .map(s -> toStepDto(s, id))
                .collect(Collectors.toList());
        dto.setSteps(stepDtos);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ExecutionSummaryDto> listRecentExecutions() {
        return executionRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    private ExecutionSummaryDto toSummaryDto(UiTestExecution e) {
        ExecutionSummaryDto dto = new ExecutionSummaryDto();
        dto.setId(e.getId());
        dto.setTestCaseId(e.getTestCaseId());
        dto.setInstanceId(e.getInstanceId());
        dto.setStatus(e.getStatus());
        dto.setStartTime(e.getStartTime());
        dto.setEndTime(e.getEndTime());
        dto.setCreatedAt(e.getCreatedAt());
        Optional<UiTestCase> tc = testCaseRepository.findById(e.getTestCaseId());
        dto.setTestCaseName(tc.map(UiTestCase::getName).orElse("（已删除用例）"));
        return dto;
    }

    @Transactional
    public void requestStop(Long id) {
        UiTestExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("执行记录不存在: " + id));
        execution.setStopRequested(true);
        execution.setUpdatedAt(Instant.now());
        executionRepository.save(execution);
    }

    private String writeOptions(ExecutionOptions options) {
        Map<String, Object> map = new HashMap<>();
        map.put("headless", options.isHeadless());
        map.put("stopOnFailure", options.isStopOnFailure());
        map.put("screenshotOnFailure", options.isScreenshotOnFailure());
        map.put("screenshotEveryStep", options.isScreenshotEveryStep());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> readOptions(String json) {
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

    private ExecutionStepDto toStepDto(UiExecutionStep step, Long executionId) {
        ExecutionStepDto dto = new ExecutionStepDto();
        dto.setIndex(step.getStepIndex());
        dto.setStepType(step.getStepType());
        dto.setAction(step.getAction());
        dto.setStatus(step.getStatus());
        dto.setStartTime(step.getStartTime());
        dto.setEndTime(step.getEndTime());
        dto.setErrorMessage(step.getErrorMessage());
        dto.setLogText(step.getLogText());
        if (step.getScreenshotPath() != null && !step.getScreenshotPath().isBlank()) {
            dto.setScreenshotUrl("/api/ui-test/executions/" + executionId + "/screenshots/" + step.getStepIndex());
        }
        return dto;
    }
}

