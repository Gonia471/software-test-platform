package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AssertStepHandler implements StepHandler {

    @Override
    public boolean supports(StepDefinition step) {
        return "assert".equalsIgnoreCase(step.getType());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        Map<String, Object> params = step.getParameters();
        String action = step.getAction();

        switch (action) {
            case "assertTitle" -> {
                String expected = (String) params.getOrDefault("expected", "");
                String actual = driver.getTitle();
                if (!expected.equals(actual)) {
                    throw new AssertionError("标题断言失败，期望: " + expected + "，实际: " + actual);
                }
                return StepResult.success("标题断言通过: " + actual);
            }
            case "assertUrl" -> {
                String expected = (String) params.getOrDefault("expected", "");
                String actual = driver.getCurrentUrl();
                if (!actual.contains(expected)) {
                    throw new AssertionError("URL 断言失败，期望包含: " + expected + "，实际: " + actual);
                }
                return StepResult.success("URL 断言通过: " + actual);
            }
            case "assertTextContains" -> {
                String expectedText = (String) params.getOrDefault("expectedText", "");
                String pageSource = driver.getPageSource();
                if (!pageSource.contains(expectedText)) {
                    throw new AssertionError("文本断言失败，未找到: " + expectedText);
                }
                return StepResult.success("文本断言通过，包含: " + expectedText);
            }
            case "assertElementExist" -> {
                boolean expected = Boolean.TRUE.equals(params.get("expected"));
                By locator = LocatorSupport.buildLocator(params, driver);
                List<WebElement> elements = driver.findElements(locator);
                boolean exists = !elements.isEmpty();
                if (exists != expected) {
                    throw new AssertionError("元素存在性断言失败，期望: " + expected + "，实际: " + exists);
                }
                return StepResult.success("元素存在性断言通过，存在: " + exists);
            }
            case "assertElementVisible" -> {
                boolean expected = Boolean.TRUE.equals(params.get("expected"));
                By locator = LocatorSupport.buildLocator(params, driver);
                List<WebElement> elements = driver.findElements(locator);
                boolean visible = !elements.isEmpty() && elements.get(0).isDisplayed();
                if (visible != expected) {
                    throw new AssertionError("元素可见性断言失败，期望: " + expected + "，实际: " + visible);
                }
                return StepResult.success("元素可见性断言通过，可见: " + visible);
            }
            default -> throw new IllegalArgumentException("不支持的断言操作: " + action);
        }
    }
}

