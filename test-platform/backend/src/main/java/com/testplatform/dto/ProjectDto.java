package com.testplatform.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDto {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Long organizationId;
    private String organizationName;
    private Long ownerId;
    private String ownerUsername;
    private String type;
    private String itemsJson;
    private String cronExpression;
    private Integer loopCount;
    private Long uiInstanceId;
    private Boolean enabled;
    private ProjectStats stats;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectStats {
        private Integer apiCollectionCount;
        private Integer uiTestCaseCount;
        private Integer environmentCount;
        private Integer scriptLibraryCount;
    }
}
