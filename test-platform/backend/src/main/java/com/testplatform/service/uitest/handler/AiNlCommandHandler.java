package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.AiActionPlannerService;
import com.testplatform.service.uitest.model.AiPlannedAction;
import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class AiNlCommandHandler implements StepHandler {

    private final AiActionPlannerService aiActionPlannerService;

    public AiNlCommandHandler(AiActionPlannerService aiActionPlannerService) {
        this.aiActionPlannerService = aiActionPlannerService;
    }

    @Override
    public boolean supports(StepDefinition step) {
        return "ai".equalsIgnoreCase(step.getType()) && "aiNaturalLanguage".equals(step.getAction());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        Map<String, Object> params = step.getParameters();
        String instruction = (String) params.getOrDefault("instruction", "");
        AiPlannedAction action = aiActionPlannerService.planFromInstruction(instruction, driver);
        String executionStrategy = performAction(action, instruction, driver);
        return StepResult.success("AI指令执行成功: " + instruction
                + " | action=" + action.getActionType()
                + " | candidateIndex=" + (action.getCandidateIndex() == null ? "-" : action.getCandidateIndex())
                + " | locator=" + buildLocatorSummary(action)
                + " | strategy=" + executionStrategy
                + " | reason=" + (action.getReason() == null ? "-" : action.getReason()));
    }

    private String performAction(AiPlannedAction action, String instruction, WebDriver driver) {
        if ("wait".equalsIgnoreCase(action.getActionType())) {
            waitForAction(action);
            return "wait";
        }
        if (action.getLocatorType() != null && action.getLocatorValue() != null) {
            By locator = LocatorSupport.buildLocator(Map.of(
                    "locatorType", action.getLocatorType(),
                    "locatorValue", action.getLocatorValue()
            ));
            WebElement element = waitForPresence(driver, locator);
            if ("input".equalsIgnoreCase(action.getActionType())) {
                scrollIntoView(driver, element);
                element.clear();
                element.sendKeys(action.getText() == null ? "" : action.getText());
                return "input";
            }
            return clickWithFallback(driver, locator, element, instruction);
        }
        if (action.getX() != null && action.getY() != null && driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "const el=document.elementFromPoint(arguments[0],arguments[1]); if(el){el.click();}",
                    action.getX(), action.getY()
            );
            return "point-click";
        }
        throw new IllegalArgumentException("AI 未返回可执行定位信息");
    }

    private WebElement waitForPresence(WebDriver driver, By locator) {
        return new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private String clickWithFallback(WebDriver driver,
                                     By locator,
                                     WebElement initialElement,
                                     String instruction) {
        if (isSearchIntent(instruction)) {
            String preferredStrategy = clickPreferredSearchSubmit(driver);
            if (preferredStrategy != null) {
                return preferredStrategy;
            }
        }

        Exception clickFailure = null;
        try {
            scrollIntoView(driver, initialElement);
            WebElement clickable = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.elementToBeClickable(locator));
            clickable.click();
            return "click";
        } catch (Exception e) {
            clickFailure = e;
        }

        try {
            WebElement fallbackElement = driver.findElement(locator);
            scrollIntoView(driver, fallbackElement);
            javascriptClick(driver, fallbackElement);
            return "js-click(" + shortError(clickFailure) + ")";
        } catch (Exception jsError) {
            if (isSearchIntent(instruction) && pressEnterOnSearchInput(driver)) {
                return "enter-fallback(click=" + shortError(clickFailure) + ", js=" + shortError(jsError) + ")";
            }
            throw buildClickFailure(locator, clickFailure, jsError);
        }
    }

    private String clickPreferredSearchSubmit(WebDriver driver) {
        for (By candidate : new By[]{
                By.id("chat-submit-button"),
                By.id("su"),
                By.cssSelector("button[type='submit']"),
                By.cssSelector("input[type='submit']"),
                By.cssSelector("[aria-label*='搜索']")
        }) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(1))
                        .until(ExpectedConditions.presenceOfElementLocated(candidate));
                scrollIntoView(driver, element);
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        element.click();
                        return "preferred-search-click:" + candidate;
                    }
                } catch (Exception clickError) {
                    javascriptClick(driver, element);
                    return "preferred-search-js-click:" + candidate + "(" + shortError(clickError) + ")";
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void scrollIntoView(WebDriver driver, WebElement element) {
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    element
            );
        }
    }

    private void javascriptClick(WebDriver driver, WebElement element) {
        if (!(driver instanceof JavascriptExecutor js)) {
            throw new IllegalStateException("当前驱动不支持 JS 点击");
        }
        js.executeScript("arguments[0].click();", element);
    }

    private boolean pressEnterOnSearchInput(WebDriver driver) {
        try {
            WebElement activeElement = driver.switchTo().activeElement();
            if (isTextInput(activeElement)) {
                activeElement.sendKeys(Keys.ENTER);
                return true;
            }
        } catch (Exception ignored) {
        }

        for (By candidate : new By[]{
                By.id("chat-textarea"),
                By.id("kw"),
                By.cssSelector("textarea"),
                By.cssSelector("input[type='text']"),
                By.cssSelector("input")
        }) {
            try {
                WebElement element = driver.findElement(candidate);
                if (!isTextInput(element)) {
                    continue;
                }
                scrollIntoView(driver, element);
                element.sendKeys(Keys.ENTER);
                return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isTextInput(WebElement element) {
        if (element == null) {
            return false;
        }
        try {
            String tagName = element.getTagName();
            String contentEditable = element.getAttribute("contenteditable");
            return element.isDisplayed()
                    && element.isEnabled()
                    && ("input".equalsIgnoreCase(tagName)
                    || "textarea".equalsIgnoreCase(tagName)
                    || "true".equalsIgnoreCase(contentEditable));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSearchIntent(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return false;
        }
        String normalized = instruction.replaceAll("\\s+", "");
        return normalized.contains("搜索")
                || normalized.contains("查询")
                || normalized.contains("百度一下")
                || normalized.contains("提交");
    }

    private IllegalStateException buildClickFailure(By locator, Exception clickFailure, Exception jsFailure) {
        String message = "AI 点击失败，locator=" + locator
                + ", clickError=" + shortError(clickFailure)
                + ", jsError=" + shortError(jsFailure);
        if (clickFailure instanceof ElementNotInteractableException
                || clickFailure instanceof TimeoutException) {
            return new IllegalStateException(message, clickFailure);
        }
        return new IllegalStateException(message, jsFailure != null ? jsFailure : clickFailure);
    }

    private String buildLocatorSummary(AiPlannedAction action) {
        if (action.getLocatorType() != null && action.getLocatorValue() != null) {
            return action.getLocatorType() + ":" + action.getLocatorValue();
        }
        if (action.getX() != null && action.getY() != null) {
            return "point(" + action.getX() + "," + action.getY() + ")";
        }
        return "-";
    }

    private String shortError(Exception e) {
        if (e == null) {
            return "-";
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        String normalized = message.replaceAll("[\\r\\n]+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }

    private void waitForAction(AiPlannedAction action) {
        int waitSeconds = action.getWaitSeconds() == null ? 2 : Math.max(1, action.getWaitSeconds());
        try {
            Thread.sleep(waitSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI 等待动作被中断", e);
        }
    }
}

