package com.testplatform.service.uitest.handler;

import com.testplatform.service.uitest.model.ExecutionContext;
import com.testplatform.service.uitest.model.StepDefinition;
import com.testplatform.service.uitest.model.StepResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ElementStepHandler implements StepHandler {

    @Override
    public boolean supports(StepDefinition step) {
        return "element".equalsIgnoreCase(step.getType());
    }

    @Override
    public StepResult execute(StepDefinition step, ExecutionContext ctx) {
        WebDriver driver = ctx.getDriver();
        Map<String, Object> params = step.getParameters();
        By locator = LocatorSupport.buildLocator(params, driver);
        WebElement el = driver.findElement(locator);

        String action = step.getAction();
        switch (action) {
            case "clickElement" -> {
                new Actions(driver).moveToElement(el).click().perform();
                return StepResult.success("点击元素");
            }
            case "inputText" -> {
                String text = (String) params.getOrDefault("text", "");
                el.clear();
                el.sendKeys(text);
                return StepResult.success("输入文本: " + text);
            }
            case "clearText" -> {
                el.clear();
                return StepResult.success("清空文本");
            }
            case "getText" -> {
                String text = el.getText();
                ctx.getVariables().put("lastText", text);
                return StepResult.success("获取文本: " + text);
            }
            case "selectOption" -> {
                Select select = new Select(el);
                String optionType = (String) params.getOrDefault("optionType", "value");
                String optionValue = (String) params.getOrDefault("optionValue", "");
                switch (optionType) {
                    case "value" -> select.selectByValue(optionValue);
                    case "text" -> select.selectByVisibleText(optionValue);
                    case "index" -> select.selectByIndex(Integer.parseInt(optionValue));
                    default -> select.selectByVisibleText(optionValue);
                }
                return StepResult.success("选择下拉选项: " + optionValue);
            }
            case "toggleCheck" -> {
                el.click();
                return StepResult.success("切换勾选状态");
            }
            default -> throw new IllegalArgumentException("不支持的元素操作: " + action);
        }
    }
}

