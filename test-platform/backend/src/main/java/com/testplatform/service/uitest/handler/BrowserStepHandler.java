package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

@Component
public class BrowserStepHandler implements StepHandler {

    @Override
    public boolean supports(StepDefinition step) {
        return "browser".equalsIgnoreCase(step.getType());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        String action = step.getAction();
        if ("openPage".equals(action)) {
            String url = (String) step.getParameters().getOrDefault("url", "");
            driver.get(url);
            return StepResult.success("打开页面: " + url);
        }
        if ("refreshPage".equals(action)) {
            driver.navigate().refresh();
            return StepResult.success("刷新页面");
        }
        if ("goBack".equals(action)) {
            driver.navigate().back();
            return StepResult.success("后退");
        }
        if ("goForward".equals(action)) {
            driver.navigate().forward();
            return StepResult.success("前进");
        }
        if ("closeWindow".equals(action)) {
            driver.close();
            return StepResult.success("关闭当前窗口");
        }
        throw new IllegalArgumentException("不支持的浏览器动作: " + action);
    }
}

