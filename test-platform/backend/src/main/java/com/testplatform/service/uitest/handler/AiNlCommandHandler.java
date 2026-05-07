package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.AiActionPlannerService;
import com.testplatform.service.uitest.model.AiPlannedAction;
import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

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
        performAction(action, driver);
        return StepResult.success("AI指令执行成功: " + instruction
                + " | action=" + action.getActionType()
                + " | reason=" + (action.getReason() == null ? "-" : action.getReason()));
    }

    private void performAction(AiPlannedAction action, WebDriver driver) {
        if (action.getLocatorType() != null && action.getLocatorValue() != null) {
            WebElement element = driver.findElement(LocatorSupport.buildLocator(Map.of(
                    "locatorType", action.getLocatorType(),
                    "locatorValue", action.getLocatorValue()
            )));
            if ("input".equalsIgnoreCase(action.getActionType())) {
                element.clear();
                element.sendKeys(action.getText() == null ? "" : action.getText());
            } else {
                element.click();
            }
            return;
        }
        if (action.getX() != null && action.getY() != null && driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "const el=document.elementFromPoint(arguments[0],arguments[1]); if(el){el.click();}",
                    action.getX(), action.getY()
            );
            return;
        }
        throw new IllegalArgumentException("AI 未返回可执行定位信息");
    }
}

