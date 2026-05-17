package com.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.dto.uitest.StartExecutionRequest;
import com.testplatform.entity.Project;
import com.testplatform.entity.User;
import com.testplatform.repository.ProjectRepository;
import com.testplatform.repository.uitest.UiExecutionInstanceRepository;
import com.testplatform.service.apitest.ApiTestExecutionEngine;
import com.testplatform.service.uitest.UiTestExecutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectExecutionService {

    private final ProjectRepository projectRepository;
    private final UiExecutionInstanceRepository instanceRepository;
    private final ApiTestExecutionEngine apiEngine;
    private final UiTestExecutionService uiExecutionService;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshAllSchedules();
    }

    public void refreshAllSchedules() {
        log.info("正在刷新所有项目的定时任务...");
        cancelAllSchedules();
        List<Project> projects = projectRepository.findAll();
        for (Project project : projects) {
            scheduleProject(project);
        }
    }

    public void scheduleProject(Project project) {
        if (project.getCronExpression() != null && !project.getCronExpression().isBlank() && Boolean.TRUE.equals(project.getEnabled())) {
            log.info("为项目 {} 设置定时任务: {}", project.getName(), project.getCronExpression());
            ScheduledFuture<?> future = taskScheduler.schedule(
                () -> runProject(project.getId(), null),
                new CronTrigger(project.getCronExpression())
            );
            scheduledTasks.put(project.getId(), future);
        }
    }

    public void cancelSchedule(Long projectId) {
        ScheduledFuture<?> future = scheduledTasks.remove(projectId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void cancelAllSchedules() {
        scheduledTasks.values().forEach(f -> f.cancel(false));
        scheduledTasks.clear();
    }

    @Transactional(readOnly = true)
    public void runProject(Long projectId, User user) {
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        User executionUser = user != null ? user : project.getOwner();
        runProjectInternal(project, executionUser, new HashSet<>());
    }

    private void runProjectInternal(Project project, User executionUser, Set<Long> visiting) {
        if (!visiting.add(project.getId())) {
            log.warn("检测到项目合集循环引用，已跳过: projectId={}, name={}", project.getId(), project.getName());
            return;
        }

        log.info("开始执行项目合集: {}, 类型: {}", project.getName(), project.getType());

        List<ProjectItemRef> items = parseItems(project);
        int loops = project.getLoopCount() != null ? project.getLoopCount() : 1;

        for (int i = 0; i < loops; i++) {
            log.info("项目 {} 第 {} 次循环开始", project.getName(), i + 1);
            for (ProjectItemRef item : items) {
                int itemLoops = item.loopCount() != null && item.loopCount() > 0 ? item.loopCount() : 1;
                for (int itemLoopIndex = 0; itemLoopIndex < itemLoops; itemLoopIndex++) {
                    try {
                        if ("API".equalsIgnoreCase(project.getType())) {
                            log.info("执行 API 用例: {}, 单项循环 {}/{}", item.itemId(), itemLoopIndex + 1, itemLoops);
                            apiEngine.execute(item.itemId(), executionUser, project.getId());
                        } else if ("UI".equalsIgnoreCase(project.getType())) {
                            log.info("执行 UI 编排项: {}, 单项循环 {}/{}", item.itemId(), itemLoopIndex + 1, itemLoops);
                            executeUiItem(project, item, executionUser, visiting);
                        }
                    } catch (Exception e) {
                        log.error("执行项目项失败: itemId={}, projectId={}, itemLoop={}", item.itemId(), project.getId(), itemLoopIndex + 1, e);
                    }
                }
            }
        }
        log.info("项目合集 {} 执行完成", project.getName());
        visiting.remove(project.getId());
    }

    private void executeUiItem(Project project, ProjectItemRef item, User executionUser, Set<Long> visiting) {
        if ("PROJECT".equals(item.itemType())) {
            Project childProject = projectRepository.findByIdWithDetails(item.itemId())
                    .orElseThrow(() -> new IllegalArgumentException("UI 项目不存在: " + item.itemId()));
            if (!"UI".equalsIgnoreCase(childProject.getType())) {
                throw new IllegalArgumentException("只能向 UI 合集中编排 UI 项目: " + item.itemId());
            }
            runProjectInternal(childProject, executionUser, visiting);
            return;
        }

        Long instanceId = item.instanceId() != null ? item.instanceId() : resolveUiInstanceId(project);
        if (instanceId == null) {
            throw new IllegalArgumentException("未配置可用的 UI 执行实例");
        }

        log.info("执行 UI 用例: {}, instanceId={}", item.itemId(), instanceId);
        StartExecutionRequest req = new StartExecutionRequest();
        req.setTestCaseId(item.itemId());
        req.setInstanceId(instanceId);
        req.setProjectId(project.getId());
        req.setHeadless(true);
        uiExecutionService.startExecution(req);
    }

    private Long resolveUiInstanceId(Project project) {
        if (project.getUiInstanceId() != null) {
            return project.getUiInstanceId();
        }
        return instanceRepository.findFirstByEnabledTrueOrderByIdAsc()
                .map(entity -> entity.getId())
                .orElse(null);
    }

    private List<ProjectItemRef> parseItems(Project project) {
        String json = project.getItemsJson();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }

            List<ProjectItemRef> items = new ArrayList<>();
            for (JsonNode node : root) {
                if (node == null || node.isNull()) {
                    continue;
                }
                if (node.isNumber()) {
                    items.add(new ProjectItemRef(
                            node.asLong(),
                            defaultItemType(project.getType()),
                            null,
                            null,
                            1));
                    continue;
                }

                Long itemId = readLong(node, "itemId");
                if (itemId == null) {
                    itemId = readLong(node, "id");
                }
                if (itemId == null) {
                    continue;
                }

                items.add(new ProjectItemRef(
                        itemId,
                        readNormalizedText(node, "itemType", defaultItemType(project.getType())),
                        readText(node, "name", null),
                        readLong(node, "instanceId"),
                        readInt(node, "itemLoopCount", readInt(node, "loopCount", 1))));
            }
            return items;
        } catch (Exception e) {
            log.error("解析项目项失败: {}", json, e);
            return List.of();
        }
    }

    private String defaultItemType(String projectType) {
        return "UI".equalsIgnoreCase(projectType) ? "CASE" : "CASE";
    }

    private Long readLong(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String readText(JsonNode node, String fieldName, String defaultValue) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? defaultValue : text.trim();
    }

    private String readNormalizedText(JsonNode node, String fieldName, String defaultValue) {
        String text = readText(node, fieldName, defaultValue);
        return text == null ? null : text.toUpperCase();
    }

    private Integer readInt(JsonNode node, String fieldName, Integer defaultValue) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (value.isInt() || value.isLong() || value.isNumber()) {
            return Math.max(1, value.asInt());
        }
        if (value.isTextual()) {
            try {
                return Math.max(1, Integer.parseInt(value.asText().trim()));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private record ProjectItemRef(Long itemId, String itemType, String name, Long instanceId, Integer loopCount) {
    }
}
