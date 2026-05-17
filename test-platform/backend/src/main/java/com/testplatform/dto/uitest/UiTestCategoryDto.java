package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UiTestCategoryDto {

    private Long id;
    private String key;
    private String name;
    private Long organizationId;
    private long caseCount;
    private boolean deletable;
    private Instant createdAt;
    private Instant updatedAt;
}
