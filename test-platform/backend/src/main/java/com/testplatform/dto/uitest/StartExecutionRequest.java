package com.testplatform.dto.uitest;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartExecutionRequest {

    private Long testCaseId;
    private Long instanceId;

    private boolean headless = true;
    private boolean stopOnFailure = false;
    private boolean screenshotOnFailure = true;
}

