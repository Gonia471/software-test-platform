package com.testplatform.service.uitest.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.service.uitest.model.AiPlannedAction;
import com.testplatform.service.uitest.model.AiPlanningRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DashScopeAiProviderClient implements AiProviderClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ui-test.ai.provider:dashscope}")
    private String provider;

    @Value("${app.ui-test.ai.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String endpoint;

    @Value("${app.ui-test.ai.api-key:}")
    private String apiKey;

    @Value("${app.ui-test.ai.model:qwen-plus}")
    private String model;

    @Value("${app.ui-test.ai.timeout-ms:30000}")
    private int timeoutMs;

    public DashScopeAiProviderClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public String getProviderName() {
        return "dashscope";
    }

    @Override
    public boolean isAvailable() {
        return "dashscope".equalsIgnoreCase(provider)
                && endpoint != null && !endpoint.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiPlannedAction plan(AiPlanningRequest request) throws Exception {
        String body = objectMapper.writeValueAsString(buildPayload(request));
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.trim()))
                .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000)))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("DashScope API 调用失败, status=" + response.statusCode() + ", body=" + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String content = readContent(contentNode);
        if (content.isBlank()) {
            throw new IllegalStateException("DashScope 未返回结构化内容");
        }
        return parseAction(content);
    }

    private Map<String, Object> buildPayload(AiPlanningRequest request) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", """
                        你是一个 Web UI 自动化动作规划器。
                        你只能根据用户指令和候选元素列表做规划，输出必须是单个 JSON 对象。
                        规则：
                        1. actionType 只能是 click、input、wait、unknown
                        2. 优先返回 candidateIndex，从候选元素列表中选择目标
                        3. 如果 actionType 是 input，必须返回 text
                        4. 如果信心不足，请返回 unknown，并把 confidence 设为较低值
                        5. 只输出 JSON，不要输出 markdown、解释、前后缀
                        JSON 字段：
                        {
                          "actionType":"click|input|wait|unknown",
                          "candidateIndex":1,
                          "locatorType":"css|xpath|id|name|linkText",
                          "locatorValue":"string",
                          "text":"string",
                          "waitSeconds":2,
                          "confidence":0.0,
                          "reason":"string"
                        }
                        """
        ));
        messages.add(Map.of(
                "role", "user",
                "content", buildPrompt(request)
        ));
        payload.put("messages", messages);
        return payload;
    }

    private String buildPrompt(AiPlanningRequest request) throws Exception {
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("scene", request.getScene());
        prompt.put("instruction", request.getInstruction());
        prompt.put("candidateElements", request.getCandidateElements());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(prompt);
    }

    private String readContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    builder.append(item.asText(""));
                    continue;
                }
                String text = item.path("text").asText("");
                if (!text.isBlank()) {
                    builder.append(text);
                }
            }
            return builder.toString();
        }
        return contentNode.toString();
    }

    private AiPlannedAction parseAction(String rawText) throws Exception {
        JsonNode node = objectMapper.readTree(extractJson(rawText));
        AiPlannedAction action = new AiPlannedAction();
        action.setActionType(node.path("actionType").asText("unknown"));
        action.setCandidateIndex(node.has("candidateIndex") && node.get("candidateIndex").isNumber()
                ? node.get("candidateIndex").asInt()
                : null);
        action.setLocatorType(readNullableText(node, "locatorType"));
        action.setLocatorValue(readNullableText(node, "locatorValue"));
        action.setText(readNullableText(node, "text"));
        action.setWaitSeconds(node.has("waitSeconds") && node.get("waitSeconds").isNumber()
                ? node.get("waitSeconds").asInt()
                : null);
        action.setConfidence(node.has("confidence") && node.get("confidence").isNumber()
                ? node.get("confidence").asDouble()
                : null);
        action.setReason(readNullableText(node, "reason"));
        return action;
    }

    private String extractJson(String rawText) {
        String trimmed = rawText == null ? "" : rawText.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return trimmed.substring(first, last + 1);
        }
        return trimmed;
    }

    private String readNullableText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value == null || value.isBlank() ? null : value;
    }
}
