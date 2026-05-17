package com.testplatform.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 跨域：本地 Vite + 部署在 Render 等平台的前端（{@code *.onrender.com}）。
 * 可选 {@code app.cors.extra-origin-patterns}（逗号分隔），用于自定义域名。
 */
@Configuration
public class WebConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.extra-origin-patterns:}") String extraPatterns) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        List<String> patterns = new ArrayList<>();
        patterns.add("http://localhost:5173");
        patterns.add("http://127.0.0.1:5173");
        patterns.add("http://localhost:5174");
        patterns.add("http://127.0.0.1:5174");
        patterns.add("http://localhost:5175");
        patterns.add("http://127.0.0.1:5175");
        patterns.add("http://localhost:[*]");
        patterns.add("http://127.0.0.1:[*]");
        patterns.add("https://*.onrender.com");
        if (StringUtils.hasText(extraPatterns)) {
            patterns.addAll(Arrays.stream(extraPatterns.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
        }
        config.setAllowedOriginPatterns(patterns);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
