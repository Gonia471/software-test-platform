package com.testplatform.service.uitest.handler;

import org.openqa.selenium.By;

import java.util.Map;

public final class LocatorSupport {

    private LocatorSupport() {
    }

    public static By buildLocator(Map<String, Object> params) {
        String type = (String) params.getOrDefault("locatorType", "css");
        String value = (String) params.getOrDefault("locatorValue", "");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("元素定位信息为空");
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
}

