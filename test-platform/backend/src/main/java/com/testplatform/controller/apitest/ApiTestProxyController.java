package com.testplatform.controller.apitest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API 测试代理：前端发起请求时通过此后端转发，避免 CORS 限制。
 */
@RestController
@RequestMapping("/api/api-test")
public class ApiTestProxyController {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @PostMapping("/send")
    public ResponseEntity<?> sendRequest(@RequestBody SendRequestDto dto) {
        if (dto == null || dto.getUrl() == null || dto.getUrl().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "URL 不能为空"));
        }
        try {
            String method = (dto.getMethod() != null && !dto.getMethod().isBlank())
                    ? dto.getMethod().toUpperCase() : "GET";

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(dto.getUrl()))
                    .timeout(Duration.ofSeconds(60));

            if (dto.getHeaders() != null) {
                for (Map.Entry<String, String> e : dto.getHeaders().entrySet()) {
                    if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null) {
                        builder.header(e.getKey(), e.getValue());
                    }
                }
            }

            if (dto.getBody() != null && !dto.getBody().isBlank()
                    && !"GET".equals(method) && !"HEAD".equals(method)) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(dto.getBody()));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            long start = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;

            Map<String, String> respHeaders = response.headers().map().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(", ", e.getValue())));

            return ResponseEntity.ok(Map.of(
                    "status", response.statusCode(),
                    "statusText", statusText(response.statusCode()),
                    "headers", respHeaders,
                    "body", response.body() != null ? response.body() : "",
                    "duration", duration,
                    "size", response.body() != null ? response.body().getBytes().length : 0
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", 0,
                    "statusText", "Error",
                    "error", e.getMessage() != null ? e.getMessage() : "请求失败",
                    "headers", Map.<String, String>of(),
                    "body", "",
                    "duration", 0,
                    "size", 0
            ));
        }
    }

    private String statusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }

    public static class SendRequestDto {
        private String method;
        private String url;
        private Map<String, String> headers;
        private String body;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}
