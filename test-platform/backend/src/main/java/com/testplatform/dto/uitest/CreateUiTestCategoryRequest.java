package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUiTestCategoryRequest {

    private Long organizationId;
    private String key;
    private String name;
}
