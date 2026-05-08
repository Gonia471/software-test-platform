package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionOptions {

    private boolean headless = true;
    private boolean stopOnFailure = false;
    private boolean screenshotOnFailure = true;
    /** 每步完成后截全页，便于报告展示执行过程（默认开启） */
    private boolean screenshotEveryStep = true;
}

