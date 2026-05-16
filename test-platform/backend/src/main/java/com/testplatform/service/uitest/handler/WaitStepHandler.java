package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class WaitStepHandler implements StepHandler {

    @Override
    public boolean supports(StepDefinition step) {
        return "wait".equalsIgnoreCase(step.getType());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        Map<String, Object> params = step.getParameters();
        String action = step.getAction();

        switch (action) {
            case "sleep" -> {
                int seconds = ((Number) params.getOrDefault("seconds", 2)).intValue();
                try {
                    Thread.sleep(seconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return StepResult.success("强制等待 " + seconds + " 秒");
            }
            case "waitVisible" -> {
                int timeout = ((Number) params.getOrDefault("timeout", 10)).intValue();
                By locator = LocatorSupport.buildLocator(params, driver);
                new WebDriverWait(driver, Duration.ofSeconds(timeout))
                        .until(ExpectedConditions.visibilityOfElementLocated(locator));
                return StepResult.success("等待元素可见");
            }
            case "waitClickable" -> {
                int timeout = ((Number) params.getOrDefault("timeout", 10)).intValue();
                By locator = LocatorSupport.buildLocator(params, driver);
                new WebDriverWait(driver, Duration.ofSeconds(timeout))
                        .until(ExpectedConditions.elementToBeClickable(locator));
                return StepResult.success("等待元素可点击");
            }
            case "waitDisappear" -> {
                int timeout = ((Number) params.getOrDefault("timeout", 10)).intValue();
                By locator = LocatorSupport.buildLocator(params, driver);
                new WebDriverWait(driver, Duration.ofSeconds(timeout))
                        .until(ExpectedConditions.invisibilityOfElementLocated(locator));
                return StepResult.success("等待元素消失");
            }
            default -> throw new IllegalArgumentException("不支持的等待操作: " + action);
        }
    }
}

