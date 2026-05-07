package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionOptions {

    private boolean headless = true;
    private boolean stopOnFailure = false;
    private boolean screenshotOnFailure = true;
}

