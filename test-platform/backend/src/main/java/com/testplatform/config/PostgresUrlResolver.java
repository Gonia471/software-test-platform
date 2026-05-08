package com.testplatform.config;

import org.springframework.core.env.Environment;

final class PostgresUrlResolver {

    private PostgresUrlResolver() {}

    /** 优先 {@code DATABASE_URL}，其次 {@code SPRING_DATASOURCE_URL}（与 Render 手写变量一致）。 */
    static String rawUrl(Environment env) {
        String a = env.getProperty("DATABASE_URL");
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        a = safeGetenv("DATABASE_URL");
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        String b = env.getProperty("SPRING_DATASOURCE_URL");
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        b = safeGetenv("SPRING_DATASOURCE_URL");
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    static String safeGetenv(String key) {
        try {
            return System.getenv(key);
        } catch (SecurityException e) {
            return null;
        }
    }
}
