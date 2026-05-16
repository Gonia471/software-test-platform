package com.testplatform.service.apitest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.apitest.ApiAssertion;
import com.testplatform.repository.apitest.ApiAssertionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssertionService {

    private final ApiAssertionRepository assertionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ApiAssertion> getAssertionsByCollectionId(Long collectionId) {
        return assertionRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
    }

    @Transactional(readOnly = true)
    public List<ApiAssertionResult> executeAssertions(
            Long collectionId,
            ApiResponse response) {

        List<ApiAssertion> assertions = assertionRepository.findByCollectionIdAndEnabledTrueOrderBySortOrderAsc(collectionId);
        List<ApiAssertionResult> results = new ArrayList<>();

        for (ApiAssertion assertion : assertions) {
            ApiAssertionResult result = executeAssertion(assertion, response);
            results.add(result);

            log.debug("断言执行结果: type={}, expression={}, passed={}",
                    assertion.getAssertionType(),
                    assertion.getExpression(),
                    result.isPassed());
        }

        return results;
    }

    private ApiAssertionResult executeAssertion(ApiAssertion assertion, ApiResponse response) {
        ApiAssertionResult result = new ApiAssertionResult();
        result.setAssertionType(assertion.getAssertionType().name());
        result.setExpression(assertion.getExpression());
        result.setExpected(assertion.getExpected());

        try {
            switch (assertion.getAssertionType()) {
                case STATUS:
                    result = executeStatusAssertion(assertion, response, result);
                    break;
                case JSONPATH:
                    result = executeJsonPathAssertion(assertion, response, result);
                    break;
                case CONTAINS:
                    result = executeContainsAssertion(assertion, response, result);
                    break;
                case DURATION:
                    result = executeDurationAssertion(assertion, response, result);
                    break;
                case HEADERS:
                    result = executeHeadersAssertion(assertion, response, result);
                    break;
            }
        } catch (Exception e) {
            result.setPassed(false);
            result.setActual("执行失败: " + e.getMessage());
            log.error("断言执行异常: {}", e.getMessage());
        }

        return result;
    }

    private ApiAssertionResult executeStatusAssertion(
            ApiAssertion assertion,
            ApiResponse response,
            ApiAssertionResult result) {

        int expectedStatus = Integer.parseInt(assertion.getExpression());
        int actualStatus = response.getStatus();

        result.setExpected(String.valueOf(expectedStatus));
        result.setActual(String.valueOf(actualStatus));
        result.setPassed(expectedStatus == actualStatus);

        return result;
    }

    private ApiAssertionResult executeJsonPathAssertion(
            ApiAssertion assertion,
            ApiResponse response,
            ApiAssertionResult result) {

        String jsonPath = assertion.getExpression();
        String expected = assertion.getExpected();

        try {
            String body = response.getBody();
            if (body == null || body.isEmpty()) {
                result.setPassed(false);
                result.setActual("响应体为空");
                return result;
            }

            JsonNode jsonNode = objectMapper.readTree(body);
            JsonNode targetNode = jsonNode.at(jsonPath.startsWith("$") ? jsonPath : "/" + jsonPath);

            if (targetNode.isMissingNode()) {
                result.setPassed(false);
                result.setActual("路径不存在: " + jsonPath);
                return result;
            }

            String actual = targetNode.isValueNode() ? targetNode.asText() : targetNode.toString();

            result.setExpected(expected);
            result.setActual(actual);

            if (expected != null && !expected.isEmpty()) {
                if (expected.startsWith("!=")) {
                    result.setPassed(!expected.substring(2).trim().equals(actual));
                } else if (expected.startsWith(">")) {
                    try {
                        double expectedNum = Double.parseDouble(expected.substring(1).trim());
                        double actualNum = Double.parseDouble(actual);
                        result.setPassed(actualNum > expectedNum);
                    } catch (NumberFormatException e) {
                        result.setPassed(actual.compareTo(expected.substring(1).trim()) > 0);
                    }
                } else if (expected.startsWith(">=")) {
                    try {
                        double expectedNum = Double.parseDouble(expected.substring(2).trim());
                        double actualNum = Double.parseDouble(actual);
                        result.setPassed(actualNum >= expectedNum);
                    } catch (NumberFormatException e) {
                        result.setPassed(actual.compareTo(expected.substring(2).trim()) >= 0);
                    }
                } else if (expected.startsWith("<")) {
                    try {
                        double expectedNum = Double.parseDouble(expected.substring(1).trim());
                        double actualNum = Double.parseDouble(actual);
                        result.setPassed(actualNum < expectedNum);
                    } catch (NumberFormatException e) {
                        result.setPassed(actual.compareTo(expected.substring(1).trim()) < 0);
                    }
                } else {
                    result.setPassed(expected.equals(actual));
                }
            } else {
                result.setPassed(true);
            }

        } catch (Exception e) {
            result.setPassed(false);
            result.setActual("解析失败: " + e.getMessage());
        }

        return result;
    }

    private ApiAssertionResult executeContainsAssertion(
            ApiAssertion assertion,
            ApiResponse response,
            ApiAssertionResult result) {

        String expected = assertion.getExpression();
        String body = response.getBody();

        result.setExpected(expected);
        result.setActual(body != null ? body : "");

        if (expected != null && body != null) {
            result.setPassed(body.contains(expected));
        } else {
            result.setPassed(false);
        }

        return result;
    }

    private ApiAssertionResult executeDurationAssertion(
            ApiAssertion assertion,
            ApiResponse response,
            ApiAssertionResult result) {

        long expected = Long.parseLong(assertion.getExpression());
        long actual = response.getDuration();

        result.setExpected(String.valueOf(expected) + "ms");
        result.setActual(String.valueOf(actual) + "ms");
        result.setPassed(actual <= expected);

        return result;
    }

    private ApiAssertionResult executeHeadersAssertion(
            ApiAssertion assertion,
            ApiResponse response,
            ApiAssertionResult result) {

        String expression = assertion.getExpression();
        String[] parts = expression.split(":");
        if (parts.length < 2) {
            result.setPassed(false);
            result.setActual("格式错误，应为 Header名称:期望值");
            return result;
        }

        String headerName = parts[0].trim();
        String expected = expression.substring(expression.indexOf(":") + 1).trim();

        Map<String, String> headers = response.getHeaders();
        String actual = headers != null ? headers.get(headerName) : null;

        result.setExpected(expected);
        result.setActual(actual != null ? actual : "(不存在)");

        if (expected.contains("*")) {
            result.setPassed(actual != null && actual.contains(expected.replace("*", "")));
        } else {
            result.setPassed(expected.equals(actual));
        }

        return result;
    }

    @Transactional
    public void deleteByCollectionId(Long collectionId) {
        assertionRepository.deleteByCollectionId(collectionId);
    }

    public static class ApiResponse {
        private int status;
        private String body;
        private Map<String, String> headers;
        private long duration;

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
    }

    public static class ApiAssertionResult {
        private String assertionType;
        private String expression;
        private String expected;
        private String actual;
        private boolean passed;

        public String getAssertionType() { return assertionType; }
        public void setAssertionType(String assertionType) { this.assertionType = assertionType; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public String getExpected() { return expected; }
        public void setExpected(String expected) { this.expected = expected; }
        public String getActual() { return actual; }
        public void setActual(String actual) { this.actual = actual; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
    }
}
