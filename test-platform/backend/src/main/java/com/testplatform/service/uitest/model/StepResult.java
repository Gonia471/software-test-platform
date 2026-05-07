package com.testplatform.service.uitest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepResult {

    private boolean success;
    private String message;
    private String screenshotPath;
    private String logText;

    public static StepResult success(String message) {
        StepResult r = new StepResult();
        r.setSuccess(true);
        r.setMessage(message);
        r.setLogText(message);
        return r;
    }

    public static StepResult failure(String message) {
        StepResult r = new StepResult();
        r.setSuccess(false);
        r.setMessage(message);
        r.setLogText(message);
        return r;
    }
}

