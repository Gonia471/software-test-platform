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
public class OrganizationDto {
    private Long id;
    private String name;
    private String description;
    private String color;
    private Long ownerId;
    private String ownerUsername;
    private Integer memberCount;
    private Integer projectCount;
    private Instant createdAt;
    private Instant updatedAt;
}