package com.testplatform.service.uitest.handler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XPathOptimizer {

    private static final Pattern POSITION_PATTERN = Pattern.compile("/(\\w+)\\[(\\d+)\\]");

    private XPathOptimizer() {
    }

    public static By optimize(WebDriver driver, String xpath) {
        if (!isAbsolutePath(xpath)) {
            return By.xpath(xpath);
        }

        String lastElementName = extractLastElementName(xpath);
        String simplified = simplifyFromLast(xpath, lastElementName);

        if (isUnique(driver, simplified)) {
            return By.xpath(simplified);
        }

        String moreComplete = simplifyWithAttributes(xpath, lastElementName);
        if (isUnique(driver, moreComplete)) {
            return By.xpath(moreComplete);
        }

        return By.xpath(xpath);
    }

    public static String optimizeXpathOnly(String xpath) {
        if (!isAbsolutePath(xpath)) {
            return xpath;
        }

        String lastElementName = extractLastElementName(xpath);
        return simplifyFromLast(xpath, lastElementName);
    }

    private static boolean isAbsolutePath(String xpath) {
        return xpath.startsWith("/html") || xpath.startsWith("/body");
    }

    private static String extractLastElementName(String xpath) {
        String[] parts = xpath.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (!part.isEmpty()) {
                return extractElementName(part);
            }
        }
        return "";
    }

    private static String extractElementName(String elementWithPosition) {
        Matcher matcher = POSITION_PATTERN.matcher("/" + elementWithPosition);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return elementWithPosition.replaceAll("\\[.*", "");
    }

    private static int extractPosition(String elementWithPosition) {
        Matcher matcher = Pattern.compile("\\[(\\d+)\\]").matcher(elementWithPosition);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    private static String simplifyFromLast(String xpath, String lastElementName) {
        String[] parts = xpath.split("/");
        LinkedList<String> tailSegments = new LinkedList<>();
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (part.isEmpty() || part.equals("html") || part.equals("body")) {
                continue;
            }

            String elementName = extractElementName(part);
            int position = extractPosition(part);
            String segment = position > 1
                    ? elementName + "[" + position + "]"
                    : elementName;

            tailSegments.addFirst(segment);

            if (elementName.equals(lastElementName)) {
                int ancestorIndex = i - 1;
                int ancestorCount = 0;
                while (ancestorIndex >= 0 && ancestorCount < 3) {
                    String ancestor = parts[ancestorIndex];
                    ancestorIndex--;
                    if (ancestor.isEmpty() || ancestor.equals("html") || ancestor.equals("body")) {
                        continue;
                    }
                    String ancestorName = extractElementName(ancestor);
                    int ancestorPosition = extractPosition(ancestor);
                    tailSegments.addFirst(ancestorPosition > 1
                            ? ancestorName + "[" + ancestorPosition + "]"
                            : ancestorName);
                    ancestorCount++;
                }
                break;
            }
        }

        if (tailSegments.isEmpty()) {
            return "//" + lastElementName;
        }
        return "//" + String.join("/", tailSegments);
    }

    private static String simplifyWithAttributes(String xpath, String lastElementName) {
        String[] parts = xpath.split("/");
        StringBuilder result = new StringBuilder("//" + lastElementName);

        int divCount = 0;
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (part.isEmpty() || part.equals("html") || part.equals("body")) {
                continue;
            }

            String elementName = extractElementName(part);

            if (elementName.equals(lastElementName)) {
                int position = extractPosition(part);
                if (position > 1) {
                    result.append("[").append(position).append("]");
                }
                break;
            }

            divCount++;
            if (divCount >= 2) {
                break;
            }
        }

        return result.toString();
    }

    private static boolean isUnique(WebDriver driver, String xpath) {
        try {
            return driver.findElements(By.xpath(xpath)).size() == 1;
        } catch (Exception e) {
            return false;
        }
    }

    public static Map<String, String> analyzeXpath(String xpath) {
        Map<String, String> analysis = new HashMap<>();
        analysis.put("original", xpath);
        analysis.put("isAbsolute", String.valueOf(isAbsolutePath(xpath)));

        if (isAbsolutePath(xpath)) {
            String lastElementName = extractLastElementName(xpath);
            String simplified = simplifyFromLast(xpath, lastElementName);
            analysis.put("optimized", simplified);
            analysis.put("elementName", lastElementName);
        } else {
            analysis.put("optimized", xpath);
            analysis.put("elementName", extractElementName(xpath));
        }

        return analysis;
    }
}
