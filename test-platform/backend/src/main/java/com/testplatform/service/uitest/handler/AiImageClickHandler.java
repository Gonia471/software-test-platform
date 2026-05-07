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

/**
 * 图像识别点击：当前阶段仅做演示实现，实际识别逻辑后续可接入 OpenCV/外部 AI 服务。
 */
@Component
public class AiImageClickHandler implements StepHandler {

    private final AiActionPlannerService aiActionPlannerService;

    public AiImageClickHandler(AiActionPlannerService aiActionPlannerService) {
        this.aiActionPlannerService = aiActionPlannerService;
    }

    @Override
    public boolean supports(StepDefinition step) {
        return "ai".equalsIgnoreCase(step.getType()) && "aiImageClick".equals(step.getAction());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        Map<String, Object> params = step.getParameters();
        String imagePath = (String) params.getOrDefault("imagePath", "");
        String instruction = (String) params.getOrDefault("instruction", "");

        AiPlannedAction action = aiActionPlannerService.planImageClick(imagePath, instruction, driver);
        performClick(action, driver);
        return StepResult.success("AI图像点击执行成功: imagePath=" + imagePath
                + " | reason=" + (action.getReason() == null ? "-" : action.getReason()));
    }

    private void performClick(AiPlannedAction action, WebDriver driver) {
        if (action.getLocatorType() != null && action.getLocatorValue() != null) {
            WebElement element = driver.findElement(LocatorSupport.buildLocator(Map.of(
                    "locatorType", action.getLocatorType(),
                    "locatorValue", action.getLocatorValue()
            )));
            element.click();
            return;
        }
        if (action.getX() != null && action.getY() != null && driver instanceof JavascriptExecutor js) {
            js.executeScript(
                    "const el=document.elementFromPoint(arguments[0],arguments[1]); if(el){el.click();}",
                    action.getX(), action.getY()
            );
            return;
        }
        throw new IllegalArgumentException("AI 未返回可执行点击目标");
    }
}

