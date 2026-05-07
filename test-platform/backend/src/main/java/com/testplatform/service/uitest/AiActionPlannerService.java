package com.testplatform.service.uitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.service.uitest.model.AiPlannedAction;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AiActionPlannerService {

    private static final Logger log = LoggerFactory.getLogger(AiActionPlannerService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ui-test.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${app.ui-test.ai.endpoint:}")
    private String aiEndpoint;

    @Value("${app.ui-test.ai.api-key:}")
    private String aiApiKey;

    @Value("${app.ui-test.ai.model:}")
    private String aiModel;

    @Value("${app.ui-test.ai.timeout-ms:20000}")
    private int timeoutMs;

    @Value("${app.ui-test.ai.min-confidence:0.7}")
    private double minConfidence;

    public AiActionPlannerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public AiPlannedAction planFromInstruction(String instruction, WebDriver driver) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalArgumentException("自然语言指令为空");
        }
        String screenshotBase64 = captureScreenshotBase64(driver);
        if (shouldUseRemoteAi()) {
            try {
                AiPlannedAction action = callRemoteAi("nl_command", instruction, null, screenshotBase64);
                if (isUsable(action)) {
                    return action;
                }
            } catch (Exception e) {
                log.warn("[AI] 远程自然语言规划失败，回退本地规则: {}", e.getMessage());
            }
        }
        return fallbackByInstruction(instruction);
    }

    public AiPlannedAction planImageClick(String imagePath, String instruction, WebDriver driver) {
        String screenshotBase64 = captureScreenshotBase64(driver);
        if (shouldUseRemoteAi()) {
            try {
                AiPlannedAction action = callRemoteAi("image_click", instruction, imagePath, screenshotBase64);
                if (isUsable(action)) {
                    return action;
                }
            } catch (Exception e) {
                log.warn("[AI] 远程图像定位失败，回退本地规则: {}", e.getMessage());
            }
        }
        // 本地兜底：优先用 instruction 关键词；其次用 imagePath 文件名猜测
        String hint = (instruction != null && !instruction.isBlank()) ? instruction : imagePath;
        return fallbackByInstruction(hint);
    }

    private boolean shouldUseRemoteAi() {
        return aiEnabled
                && aiEndpoint != null && !aiEndpoint.isBlank()
                && aiApiKey != null && !aiApiKey.isBlank();
    }

    private AiPlannedAction callRemoteAi(String scene,
                                         String instruction,
                                         String imagePath,
                                         String screenshotBase64) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("scene", scene);
        payload.put("instruction", instruction == null ? "" : instruction);
        payload.put("imagePath", imagePath == null ? "" : imagePath);
        payload.put("model", aiModel == null ? "" : aiModel);
        payload.put("screenshotBase64", screenshotBase64);
        payload.put("outputFormat", "json");
        payload.put("outputSchema", """
                {
                  "actionType": "click|input|wait|unknown",
                  "locatorType": "css|xpath|id|name|linkText",
                  "locatorValue": "string",
                  "x": 0,
                  "y": 0,
                  "text": "string",
                  "confidence": 0.0,
                  "reason": "string"
                }
                """);

        String body = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiEndpoint.trim()))
                .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("AI API 调用失败, status=" + response.statusCode());
        }
        return parseAction(response.body());
    }

    private AiPlannedAction parseAction(String jsonText) throws Exception {
        JsonNode root = objectMapper.readTree(jsonText);
        // 支持 {"data":{...}} / {"result":{...}} / 直接 {...}
        JsonNode node = root.has("data") ? root.get("data")
                : root.has("result") ? root.get("result")
                : root;

        AiPlannedAction action = new AiPlannedAction();
        action.setActionType(node.path("actionType").asText("unknown"));
        action.setLocatorType(readNullableText(node, "locatorType"));
        action.setLocatorValue(readNullableText(node, "locatorValue"));
        if (node.has("x") && node.get("x").isNumber()) {
            action.setX(node.get("x").asInt());
        }
        if (node.has("y") && node.get("y").isNumber()) {
            action.setY(node.get("y").asInt());
        }
        action.setText(readNullableText(node, "text"));
        if (node.has("confidence") && node.get("confidence").isNumber()) {
            action.setConfidence(node.get("confidence").asDouble());
        }
        action.setReason(readNullableText(node, "reason"));
        return action;
    }

    private String readNullableText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? null : value;
    }

    private boolean isUsable(AiPlannedAction action) {
        if (action == null) {
            return false;
        }
        if (action.getConfidence() != null && action.getConfidence() < minConfidence) {
            return false;
        }
        return hasLocator(action) || hasPoint(action);
    }

    private AiPlannedAction fallbackByInstruction(String instruction) {
        String keyword = extractKeyword(instruction == null ? "" : instruction);
        AiPlannedAction action = new AiPlannedAction();
        action.setActionType("click");
        action.setLocatorType("xpath");
        action.setLocatorValue(String.format(
                "//*[self::button or self::a or self::span or self::div][contains(normalize-space(.),'%s')]",
                keyword
        ));
        action.setConfidence(0.5);
        action.setReason("本地规则回退，按关键词点击");
        return action;
    }

    private String extractKeyword(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isBlank()) {
            return "确定";
        }
        int idx = t.indexOf("点击");
        if (idx >= 0 && idx + 2 < t.length()) {
            String rest = t.substring(idx + 2).replace("按钮", "").replace("\"", "").replace("“", "").replace("”", "").trim();
            if (rest.length() >= 2) {
                return rest.substring(0, Math.min(rest.length(), 6));
            }
        }
        String[] parts = t.split("\\s+");
        String p = parts[0].toLowerCase(Locale.ROOT);
        return p.length() > 6 ? p.substring(0, 6) : p;
    }

    private boolean hasLocator(AiPlannedAction action) {
        return action.getLocatorType() != null && !action.getLocatorType().isBlank()
                && action.getLocatorValue() != null && !action.getLocatorValue().isBlank();
    }

    private boolean hasPoint(AiPlannedAction action) {
        return action.getX() != null && action.getY() != null;
    }

    private String captureScreenshotBase64(WebDriver driver) {
        if (!(driver instanceof TakesScreenshot ts)) {
            return "";
        }
        try {
            return ts.getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.debug("[AI] 截图失败: {}", e.getMessage());
            return "";
        }
    }
}
