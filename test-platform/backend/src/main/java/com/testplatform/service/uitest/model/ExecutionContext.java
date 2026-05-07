package com.testplatform.service.uitest.model;

import com.testplatform.entity.uitest.UiTestExecution;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionContext {

    private final UiTestExecution execution;
    private final WebDriver driver;
    private final ExecutionOptions options;
    private final AtomicBoolean stopRequested;
    private final Path screenshotRoot;
    private final Map<String, Object> variables = new HashMap<>();

    public ExecutionContext(UiTestExecution execution,
                            WebDriver driver,
                            ExecutionOptions options,
                            AtomicBoolean stopRequested,
                            Path screenshotRoot) {
        this.execution = execution;
        this.driver = driver;
        this.options = options;
        this.stopRequested = stopRequested;
        this.screenshotRoot = screenshotRoot;
    }

    public UiTestExecution getExecution() {
        return execution;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public ExecutionOptions getOptions() {
        return options;
    }

    public AtomicBoolean getStopRequested() {
        return stopRequested;
    }

    public Path getScreenshotRoot() {
        return screenshotRoot;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }
}

