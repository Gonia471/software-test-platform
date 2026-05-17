package com.testplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseSpaceDto {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Long ownerId;
    private String ownerUsername;
    private Integer memberCount;
    private Integer organizationCount;
    private Instant createdAt;
    private Instant updatedAt;
}
