package com.testplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDto {

    private ScopeCard scope;
    private Metrics metrics;
    private ResourceHealth resourceHealth;
    @Builder.Default
    private List<TrendPoint> trends = new ArrayList<>();
    @Builder.Default
    private List<ActivityItem> recentActivities = new ArrayList<>();
    @Builder.Default
    private List<ResourceUpdateItem> recentUpdates = new ArrayList<>();
    @Builder.Default
    private List<FocusItem> focusItems = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeCard {
        private Long organizationId;
        private String organizationName;
        private String organizationDescription;
        private String organizationColor;
        private String ownerUsername;
        private Integer accessibleOrganizationCount;
        private Integer memberCount;
        private Integer projectCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private Long apiCaseCount;
        private Long uiCaseCount;
        private Integer projectCount;
        private Integer scheduledProjectCount;
        private Long executionCount7d;
        private Integer successRate7d;
        private Long failedCount7d;
        private Instant latestExecutionAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceHealth {
        private Integer apiProjectCount;
        private Integer uiProjectCount;
        private Long apiFolderCount;
        private Long apiCaseCount;
        private Long uiCaseCount;
        private Integer enabledProjectCount;
        private Long apiExecutionCount7d;
        private Long uiExecutionCount7d;
        private Long passedCount7d;
        private Long failedCount7d;
        private Long runningCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String dateKey;
        private String label;
        private Long apiCount;
        private Long uiCount;
        private Long successCount;
        private Long failedCount;
        private Long totalCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String source;
        private String status;
        private String title;
        private String description;
        private Instant time;
        private String route;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUpdateItem {
        private String category;
        private String name;
        private String summary;
        private Instant updatedAt;
        private String route;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FocusItem {
        private String level;
        private String title;
        private String description;
        private String route;
    }
}
