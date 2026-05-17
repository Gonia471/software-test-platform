package com.testplatform.service;

import com.testplatform.dto.DashboardOverviewDto;
import com.testplatform.entity.Organization;
import com.testplatform.entity.Project;
import com.testplatform.entity.User;
import com.testplatform.entity.apitest.ApiCollection;
import com.testplatform.entity.apitest.ApiTestExecution;
import com.testplatform.entity.uitest.UiTestCase;
import com.testplatform.entity.uitest.UiTestExecution;
import com.testplatform.repository.OrganizationMemberRepository;
import com.testplatform.repository.OrganizationRepository;
import com.testplatform.repository.ProjectRepository;
import com.testplatform.repository.apitest.ApiCollectionRepository;
import com.testplatform.repository.apitest.ApiTestExecutionRepository;
import com.testplatform.repository.uitest.UiTestCaseRepository;
import com.testplatform.repository.uitest.UiTestExecutionRepository;
import com.testplatform.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d", Locale.CHINA);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectRepository projectRepository;
    private final ApiCollectionRepository apiCollectionRepository;
    private final ApiTestExecutionRepository apiTestExecutionRepository;
    private final UiTestCaseRepository uiTestCaseRepository;
    private final UiTestExecutionRepository uiTestExecutionRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewDto getOverview(Long organizationId, User user) {
        List<Organization> accessibleOrganizations = loadAccessibleOrganizations(user);
        Organization currentOrganization = resolveCurrentOrganization(organizationId, accessibleOrganizations);

        if (currentOrganization == null) {
            return DashboardOverviewDto.builder()
                    .scope(DashboardOverviewDto.ScopeCard.builder()
                            .accessibleOrganizationCount(0)
                            .memberCount(0)
                            .projectCount(0)
                            .build())
                    .metrics(emptyMetrics())
                    .resourceHealth(emptyResourceHealth())
                    .trends(buildEmptyTrends())
                    .focusItems(List.of(
                            DashboardOverviewDto.FocusItem.builder()
                                    .level("info")
                                    .title("还没有可访问的组织")
                                    .description("请先创建组织或加入已有组织，首页才会开始展示业务数据。")
                                    .route("/organizations")
                                    .build()
                    ))
                    .build();
        }

        Long orgId = currentOrganization.getId();
        int accessibleOrganizationCount = accessibleOrganizations.size();
        int memberCount = organizationMemberRepository.countByOrganizationId(orgId);
        int projectCount = projectRepository.countByOrganizationId(orgId);
        int apiProjectCount = projectRepository.countByOrganizationIdAndType(orgId, "API");
        int uiProjectCount = projectRepository.countByOrganizationIdAndType(orgId, "UI");
        int enabledProjectCount = projectRepository.countEnabledByOrganizationId(orgId);
        int scheduledProjectCount = projectRepository.countScheduledByOrganizationId(orgId);
        long apiCaseCount = apiCollectionRepository.countByOrganizationIdAndNodeType(orgId, ApiCollection.NodeType.CASE);
        long apiFolderCount = apiCollectionRepository.countByOrganizationIdAndNodeType(orgId, ApiCollection.NodeType.FOLDER);
        long uiCaseCount = uiTestCaseRepository.countByOrganizationId(orgId);

        Set<Long> orgProjectIds = projectRepository.findIdsByOrganizationId(orgId).stream().collect(Collectors.toSet());
        Set<Long> orgApiCaseIds = apiCollectionRepository.findIdsByOrganizationIdAndNodeType(orgId, ApiCollection.NodeType.CASE)
                .stream().collect(Collectors.toSet());
        Set<Long> orgUiCaseIds = uiTestCaseRepository.findIdsByOrganizationId(orgId).stream().collect(Collectors.toSet());

        Instant since = Instant.now().minusSeconds(7L * 24 * 60 * 60);
        List<ApiTestExecution> apiExecutions7d = apiTestExecutionRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since)
                .stream()
                .filter(execution -> belongsToOrganization(execution, orgProjectIds, orgApiCaseIds))
                .toList();
        List<UiTestExecution> uiExecutions7d = uiTestExecutionRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since)
                .stream()
                .filter(execution -> belongsToOrganization(execution, orgProjectIds, orgUiCaseIds))
                .toList();

        long apiExecutionCount7d = apiExecutions7d.size();
        long uiExecutionCount7d = uiExecutions7d.size();
        long totalExecutions7d = apiExecutionCount7d + uiExecutionCount7d;
        long apiPassedCount = apiExecutions7d.stream()
                .filter(execution -> execution.getStatus() == ApiTestExecution.TestStatus.SUCCESS)
                .count();
        long uiPassedCount = uiExecutions7d.stream()
                .filter(execution -> "PASSED".equalsIgnoreCase(execution.getStatus()))
                .count();
        long passedCount7d = apiPassedCount + uiPassedCount;
        long apiFailedCount = apiExecutions7d.stream()
                .filter(execution -> execution.getStatus() == ApiTestExecution.TestStatus.FAILED
                        || execution.getStatus() == ApiTestExecution.TestStatus.ERROR)
                .count();
        long uiFailedCount = uiExecutions7d.stream()
                .filter(execution -> "FAILED".equalsIgnoreCase(execution.getStatus()))
                .count();
        long failedCount7d = apiFailedCount + uiFailedCount;
        long runningCount = uiExecutions7d.stream()
                .filter(execution -> "RUNNING".equalsIgnoreCase(execution.getStatus())
                        || "PENDING".equalsIgnoreCase(execution.getStatus()))
                .count();

        Instant latestExecutionAt = Stream.concat(
                        apiExecutions7d.stream().map(ApiTestExecution::getCreatedAt),
                        uiExecutions7d.stream().map(UiTestExecution::getCreatedAt)
                )
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        int successRate7d = totalExecutions7d == 0
                ? 0
                : (int) Math.round((passedCount7d * 100.0) / totalExecutions7d);

        return DashboardOverviewDto.builder()
                .scope(DashboardOverviewDto.ScopeCard.builder()
                        .organizationId(orgId)
                        .organizationName(currentOrganization.getName())
                        .organizationDescription(currentOrganization.getDescription())
                        .organizationColor(currentOrganization.getColor())
                        .ownerUsername(currentOrganization.getOwner() != null ? currentOrganization.getOwner().getUsername() : null)
                        .accessibleOrganizationCount(accessibleOrganizationCount)
                        .memberCount(memberCount)
                        .projectCount(projectCount)
                        .build())
                .metrics(DashboardOverviewDto.Metrics.builder()
                        .apiCaseCount(apiCaseCount)
                        .uiCaseCount(uiCaseCount)
                        .projectCount(projectCount)
                        .scheduledProjectCount(scheduledProjectCount)
                        .executionCount7d(totalExecutions7d)
                        .successRate7d(successRate7d)
                        .failedCount7d(failedCount7d)
                        .latestExecutionAt(latestExecutionAt)
                        .build())
                .resourceHealth(DashboardOverviewDto.ResourceHealth.builder()
                        .apiProjectCount(apiProjectCount)
                        .uiProjectCount(uiProjectCount)
                        .apiFolderCount(apiFolderCount)
                        .apiCaseCount(apiCaseCount)
                        .uiCaseCount(uiCaseCount)
                        .enabledProjectCount(enabledProjectCount)
                        .apiExecutionCount7d(apiExecutionCount7d)
                        .uiExecutionCount7d(uiExecutionCount7d)
                        .passedCount7d(passedCount7d)
                        .failedCount7d(failedCount7d)
                        .runningCount(runningCount)
                        .build())
                .trends(List.of())
                .recentActivities(List.of())
                .recentUpdates(List.of())
                .focusItems(List.of())
                .build();
    }

    private List<Organization> loadAccessibleOrganizations(User user) {
        if (SecurityUtils.isDevMode()) {
            return organizationRepository.findAll();
        }
        return organizationRepository.findAllByMember(user);
    }

    private Organization resolveCurrentOrganization(Long organizationId, List<Organization> accessibleOrganizations) {
        if (accessibleOrganizations.isEmpty()) {
            return null;
        }
        if (organizationId == null) {
            return accessibleOrganizations.get(0);
        }
        return accessibleOrganizations.stream()
                .filter(org -> org.getId().equals(organizationId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限访问该组织仪表盘"));
    }

    private boolean belongsToOrganization(ApiTestExecution execution, Set<Long> orgProjectIds, Set<Long> orgApiCaseIds) {
        if (execution.getProjectId() != null && orgProjectIds.contains(execution.getProjectId())) {
            return true;
        }
        Long collectionId = parseLong(execution.getCollectionId());
        return collectionId != null && orgApiCaseIds.contains(collectionId);
    }

    private boolean belongsToOrganization(UiTestExecution execution, Set<Long> orgProjectIds, Set<Long> orgUiCaseIds) {
        if (execution.getProjectId() != null && orgProjectIds.contains(execution.getProjectId())) {
            return true;
        }
        return execution.getTestCaseId() != null && orgUiCaseIds.contains(execution.getTestCaseId());
    }

    private List<DashboardOverviewDto.TrendPoint> buildTrends(List<ApiTestExecution> apiExecutions, List<UiTestExecution> uiExecutions) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        Map<LocalDate, TrendAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            accumulatorMap.put(date, new TrendAccumulator());
        }

        for (ApiTestExecution execution : apiExecutions) {
            LocalDate date = execution.getCreatedAt().atZone(DEFAULT_ZONE).toLocalDate();
            TrendAccumulator accumulator = accumulatorMap.get(date);
            if (accumulator == null) {
                continue;
            }
            accumulator.apiCount++;
            accumulator.totalCount++;
            if (execution.getStatus() == ApiTestExecution.TestStatus.SUCCESS) {
                accumulator.successCount++;
            } else if (execution.getStatus() == ApiTestExecution.TestStatus.FAILED
                    || execution.getStatus() == ApiTestExecution.TestStatus.ERROR) {
                accumulator.failedCount++;
            }
        }

        for (UiTestExecution execution : uiExecutions) {
            LocalDate date = execution.getCreatedAt().atZone(DEFAULT_ZONE).toLocalDate();
            TrendAccumulator accumulator = accumulatorMap.get(date);
            if (accumulator == null) {
                continue;
            }
            accumulator.uiCount++;
            accumulator.totalCount++;
            if ("PASSED".equalsIgnoreCase(execution.getStatus())) {
                accumulator.successCount++;
            } else if ("FAILED".equalsIgnoreCase(execution.getStatus())) {
                accumulator.failedCount++;
            }
        }

        return accumulatorMap.entrySet().stream()
                .map(entry -> DashboardOverviewDto.TrendPoint.builder()
                        .dateKey(entry.getKey().toString())
                        .label(entry.getKey().format(TREND_LABEL_FORMATTER))
                        .apiCount(entry.getValue().apiCount)
                        .uiCount(entry.getValue().uiCount)
                        .successCount(entry.getValue().successCount)
                        .failedCount(entry.getValue().failedCount)
                        .totalCount(entry.getValue().totalCount)
                        .build())
                .toList();
    }

    private List<DashboardOverviewDto.ActivityItem> buildRecentActivities(
            Long userId,
            Set<Long> orgProjectIds,
            Set<Long> orgApiCaseIds,
            Set<Long> orgUiCaseIds
    ) {
        List<DashboardOverviewDto.ActivityItem> items = new ArrayList<>();

        apiTestExecutionRepository.findRecentByUserId(userId, PageRequest.of(0, 8)).stream()
                .filter(execution -> belongsToOrganization(execution, orgProjectIds, orgApiCaseIds))
                .map(this::toApiActivity)
                .forEach(items::add);

        uiTestExecutionRepository.findTop200ByOrderByCreatedAtDesc().stream()
                .filter(execution -> belongsToOrganization(execution, orgProjectIds, orgUiCaseIds))
                .limit(8)
                .map(this::toUiActivity)
                .forEach(items::add);

        return items.stream()
                .sorted(Comparator.comparing(DashboardOverviewDto.ActivityItem::getTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();
    }

    private DashboardOverviewDto.ActivityItem toApiActivity(ApiTestExecution execution) {
        String status = switch (execution.getStatus()) {
            case SUCCESS -> "success";
            case FAILED, ERROR -> "danger";
        };
        String statusText = switch (execution.getStatus()) {
            case SUCCESS -> "执行成功";
            case FAILED -> "断言失败";
            case ERROR -> "执行异常";
        };
        return DashboardOverviewDto.ActivityItem.builder()
                .source("API")
                .status(status)
                .title("API 用例执行：" + defaultText(execution.getCollectionName(), "未命名 API 用例"))
                .description(statusText + "，HTTP " + defaultNumber(execution.getHttpStatus()) + "，耗时 " + defaultNumber(execution.getDuration()) + " ms")
                .time(execution.getCreatedAt())
                .route("/reports")
                .build();
    }

    private DashboardOverviewDto.ActivityItem toUiActivity(UiTestExecution execution) {
        String normalizedStatus = defaultText(execution.getStatus(), "UNKNOWN").toUpperCase(Locale.ROOT);
        String tag = switch (normalizedStatus) {
            case "PASSED" -> "success";
            case "FAILED" -> "danger";
            case "RUNNING", "PENDING" -> "warning";
            default -> "info";
        };
        String text = switch (normalizedStatus) {
            case "PASSED" -> "执行通过";
            case "FAILED" -> "执行失败";
            case "RUNNING" -> "执行中";
            case "PENDING" -> "等待执行";
            case "STOPPED" -> "已停止";
            default -> normalizedStatus;
        };
        return DashboardOverviewDto.ActivityItem.builder()
                .source("UI")
                .status(tag)
                .title("UI 用例执行：#" + execution.getTestCaseId())
                .description(text + "，实例 #" + execution.getInstanceId())
                .time(execution.getCreatedAt())
                .route("/reports")
                .build();
    }

    private List<DashboardOverviewDto.ResourceUpdateItem> buildRecentUpdates(Long orgId) {
        List<DashboardOverviewDto.ResourceUpdateItem> items = new ArrayList<>();

        projectRepository.findRecentByOrganizationId(orgId, PageRequest.of(0, 4)).stream()
                .map(project -> DashboardOverviewDto.ResourceUpdateItem.builder()
                        .category("合集")
                        .name(project.getName())
                        .summary(defaultText(project.getType(), "未知类型") + " 合集，负责人 " + defaultText(project.getOwner() != null ? project.getOwner().getUsername() : null, "未知"))
                        .updatedAt(project.getUpdatedAt())
                        .route("/projects")
                        .build())
                .forEach(items::add);

        apiCollectionRepository.findRecentByOrganizationIdAndNodeType(orgId, ApiCollection.NodeType.CASE, PageRequest.of(0, 4)).stream()
                .map(collection -> DashboardOverviewDto.ResourceUpdateItem.builder()
                        .category("API 用例")
                        .name(collection.getName())
                        .summary(defaultText(collection.getMethod(), "未设置方法") + " " + abbreviate(collection.getUrl(), 56))
                        .updatedAt(collection.getUpdatedAt())
                        .route("/api-test")
                        .build())
                .forEach(items::add);

        uiTestCaseRepository.findRecentByOrganizationId(orgId, PageRequest.of(0, 4)).stream()
                .map(testCase -> DashboardOverviewDto.ResourceUpdateItem.builder()
                        .category("UI 用例")
                        .name(testCase.getName())
                        .summary("分类：" + defaultText(testCase.getModuleKey(), "未分类"))
                        .updatedAt(testCase.getUpdatedAt())
                        .route("/ui-test")
                        .build())
                .forEach(items::add);

        return items.stream()
                .sorted(Comparator.comparing(DashboardOverviewDto.ResourceUpdateItem::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();
    }

    private List<DashboardOverviewDto.FocusItem> buildFocusItems(
            long apiCaseCount,
            long uiCaseCount,
            int scheduledProjectCount,
            int memberCount,
            List<ApiTestExecution> apiExecutions,
            List<UiTestExecution> uiExecutions
    ) {
        List<DashboardOverviewDto.FocusItem> items = new ArrayList<>();
        long failedCount = apiExecutions.stream()
                .filter(execution -> execution.getStatus() == ApiTestExecution.TestStatus.FAILED
                        || execution.getStatus() == ApiTestExecution.TestStatus.ERROR)
                .count()
                + uiExecutions.stream().filter(execution -> "FAILED".equalsIgnoreCase(execution.getStatus())).count();

        if (apiCaseCount == 0) {
            items.add(focus("warning", "当前组织暂无 API 用例", "建议先在 API 测试页沉淀核心接口用例，便于后续执行与报告展示。", "/api-test"));
        }
        if (uiCaseCount == 0) {
            items.add(focus("warning", "当前组织暂无 UI 用例", "建议先补充关键业务流程的 UI 自动化用例，首页执行质量才会更完整。", "/ui-test"));
        }
        if (scheduledProjectCount == 0) {
            items.add(focus("info", "尚未配置定时合集", "可在项目管理中为 API/UI 合集设置定时执行，形成持续回归能力。", "/projects"));
        }
        if (memberCount <= 1) {
            items.add(focus("info", "当前组织协作人数较少", "可以邀请成员加入组织，逐步形成用例维护、执行与分析的协作闭环。", "/organizations"));
        }
        if (failedCount > 0) {
            items.add(focus("danger", "近 7 天存在失败执行", "建议优先查看测试报告中的失败记录，排查环境、断言或定位问题。", "/reports"));
        }
        if (items.isEmpty()) {
            items.add(focus("success", "当前组织状态良好", "资源、执行和协作数据都已经建立，可以继续补充更多自动化覆盖范围。", "/reports"));
        }
        return items.stream().limit(5).toList();
    }

    private DashboardOverviewDto.FocusItem focus(String level, String title, String description, String route) {
        return DashboardOverviewDto.FocusItem.builder()
                .level(level)
                .title(title)
                .description(description)
                .route(route)
                .build();
    }

    private DashboardOverviewDto.Metrics emptyMetrics() {
        return DashboardOverviewDto.Metrics.builder()
                .apiCaseCount(0L)
                .uiCaseCount(0L)
                .projectCount(0)
                .scheduledProjectCount(0)
                .executionCount7d(0L)
                .successRate7d(0)
                .failedCount7d(0L)
                .build();
    }

    private DashboardOverviewDto.ResourceHealth emptyResourceHealth() {
        return DashboardOverviewDto.ResourceHealth.builder()
                .apiProjectCount(0)
                .uiProjectCount(0)
                .apiFolderCount(0L)
                .apiCaseCount(0L)
                .uiCaseCount(0L)
                .enabledProjectCount(0)
                .apiExecutionCount7d(0L)
                .uiExecutionCount7d(0L)
                .passedCount7d(0L)
                .failedCount7d(0L)
                .runningCount(0L)
                .build();
    }

    private List<DashboardOverviewDto.TrendPoint> buildEmptyTrends() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        List<DashboardOverviewDto.TrendPoint> items = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            items.add(DashboardOverviewDto.TrendPoint.builder()
                    .dateKey(date.toString())
                    .label(date.format(TREND_LABEL_FORMATTER))
                    .apiCount(0L)
                    .uiCount(0L)
                    .successCount(0L)
                    .failedCount(0L)
                    .totalCount(0L)
                    .build());
        }
        return items;
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultNumber(Number value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "未配置 URL";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 1)) + "...";
    }

    private static class TrendAccumulator {
        long apiCount;
        long uiCount;
        long successCount;
        long failedCount;
        long totalCount;
    }
}
