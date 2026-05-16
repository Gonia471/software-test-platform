package com.testplatform.service.uitest.handler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public final class LocatorSupport {

    private LocatorSupport() {
    }

    public static By buildLocator(Map<String, Object> params) {
        return buildLocator(params, null);
    }

    public static By buildLocator(Map<String, Object> params, WebDriver driver) {
        String type = (String) params.getOrDefault("locatorType", "css");
        String value = (String) params.getOrDefault("locatorValue", "");
        Boolean optimizeXpath = (Boolean) params.getOrDefault("optimizeXpath", false);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("元素定位信息为空");
        }

        if ("xpath".equals(type) && Boolean.TRUE.equals(optimizeXpath) && driver != null) {
            return XPathOptimizer.optimize(driver, value);
        }

        return switch (type) {
            case "xpath" -> By.xpath(value);
            case "id" -> By.id(value);
            case "name" -> By.name(value);
            case "linkText" -> By.linkText(value);
            case "css" -> By.cssSelector(value);
            default -> By.cssSelector(value);
        };
    }

    public static By buildLocatorWithOptimization(Map<String, Object> params, WebDriver driver, boolean optimizeXpath) {
        String type = (String) params.getOrDefault("locatorType", "css");
        String value = (String) params.getOrDefault("locatorValue", "");

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("元素定位信息为空");
        }

        if ("xpath".equals(type) && optimizeXpath) {
            return XPathOptimizer.optimize(driver, value);
        }

        return buildLocator(params);
    }

    public static String getOptimizedXpath(String xpath) {
        return XPathOptimizer.optimizeXpathOnly(xpath);
    }

    public static Map<String, String> analyzeXpath(String xpath) {
        return XPathOptimizer.analyzeXpath(xpath);
    }
}

