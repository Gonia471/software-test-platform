package com.testplatform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查，用于确认后端已启动。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * 简单健康检查接口：
     * 用于探活，判断后端服务是否正常启动与响应。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "software-test-platform"
        );
    }
}
