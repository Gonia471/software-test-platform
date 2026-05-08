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
    /**
     * 每步完成后截图；缺省或未传视为 true（Jackson 对 boolean 缺省会变成 false，故用包装类型）
     */
    private Boolean screenshotEveryStep;
}

