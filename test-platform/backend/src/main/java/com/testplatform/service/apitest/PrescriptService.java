package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.apitest.ApiPrescript;
import com.testplatform.repository.apitest.ApiPrescriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptService {

    private final ApiPrescriptRepository prescriptRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Transactional(readOnly = true)
    public List<ApiPrescript> getPrescriptsByCollectionId(Long collectionId) {
        return prescriptRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
    }

    public PrescriptResult executeHttpRequest(
            ApiPrescript prescript,
            Map<String, String> variables) {

        PrescriptResult result = new PrescriptResult();
        result.setStepType("HTTP");
        result.setMethod(prescript.getMethod());
        result.setUrl(prescript.getUrl());

        try {
            String url = replaceVariables(prescript.getUrl(), variables);
            Map<String, String> headers = parseJson(prescript.getHeadersJson(), new TypeReference<>() {});
            String body = prescript.getBodyJson();
            if (body != null && !body.isEmpty()) {
                body = replaceVariables(body, variables);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60));

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String value = replaceVariables(entry.getValue(), variables);
                    requestBuilder.header(entry.getKey(), value);
                }
            }

            String method = prescript.getMethod();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                if (body != null && !body.isEmpty()) {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(body));
                } else {
                    requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
                }
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            long duration = System.currentTimeMillis() - start;

            result.setStatus(response.statusCode());
            result.setBody(response.body());
            result.setDuration(duration);
            result.setSuccess(true);

            Map<String, String> responseHeaders = new HashMap<>();
            response.headers().map().forEach((key, values) -> 
                    responseHeaders.put(key, String.join(", ", values)));
            result.setHeaders(responseHeaders);

            Map<String, String> extractedVars = extractVariables(prescript.getExtractParamsJson(), response.body());
            result.setExtractedVariables(extractedVars);

            log.info("前置HTTP执行成功: {} {} -> {}", method, url, response.statusCode());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("前置HTTP执行失败: {} {} -> {}", prescript.getMethod(), prescript.getUrl(), e.getMessage());
        }

        return result;
    }

    public Map<String, String> extractVariables(String extractParamsJson, String responseBody) {
        Map<String, String> extracted = new HashMap<>();
        
        if (extractParamsJson == null || extractParamsJson.isEmpty()) {
            return extracted;
        }

        try {
            List<Map<String, String>> extractParams = parseJson(extractParamsJson, new TypeReference<>() {});
            if (extractParams == null) {
                return extracted;
            }

            for (Map<String, String> param : extractParams) {
                String varName = param.get("name");
                String path = param.get("path");

                if (varName == null || varName.isEmpty() || path == null || path.isEmpty()) {
                    continue;
                }

                String value = extractJsonPath(responseBody, path);
                if (value != null) {
                    extracted.put(varName, value);
                    log.debug("提取变量: {} = {}", varName, value);
                }
            }
        } catch (Exception e) {
            log.error("提取变量失败: {}", e.getMessage());
        }

        return extracted;
    }

    private String extractJsonPath(String json, String path) {
        if (json == null || json.isEmpty() || path == null || path.isEmpty()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            String jsonPath = path.startsWith("$") ? path : "$" + path;
            JsonNode target = root.at(jsonPath);
            
            if (target.isMissingNode() || target.isNull()) {
                return null;
            }
            
            return target.isValueNode() ? target.asText() : target.toString();
        } catch (Exception e) {
            log.warn("JSONPath提取失败: path={}, error={}", path, e.getMessage());
            return null;
        }
    }

    public String replaceVariables(String text, Map<String, String> variables) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void deleteByCollectionId(Long collectionId) {
        prescriptRepository.deleteByCollectionId(collectionId);
    }

    public static class PrescriptResult {
        private String stepType;
        private String method;
        private String url;
        private int status;
        private String body;
        private long duration;
        private boolean success;
        private String errorMessage;
        private Map<String, String> headers;
        private Map<String, String> extractedVariables;

        public String getStepType() { return stepType; }
        public void setStepType(String stepType) { this.stepType = stepType; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public Map<String, String> getExtractedVariables() { return extractedVariables; }
        public void setExtractedVariables(Map<String, String> extractedVariables) { this.extractedVariables = extractedVariables; }
    }
}
