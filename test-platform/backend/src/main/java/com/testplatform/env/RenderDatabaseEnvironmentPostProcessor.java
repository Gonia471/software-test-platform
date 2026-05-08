package com.testplatform.env;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 将 Render/Heroku 风格的 {@code DATABASE_URL}/{@code SPRING_DATASOURCE_URL}（{@code postgres://...}）
 * 转为 {@code jdbc:postgresql://...}。注意：若在面板填了 {@code SPRING_DATASOURCE_URL} 但值仍是
 * {@code postgresql://}（而非 {@code jdbc:postgresql:}），必须参与转换，否则 Spring 无法用其建连，
 * Hibernate 会报 Unable to determine Dialect。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class RenderDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "postgresDatabaseUrlFromEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String fromDbUrl =
                firstNonBlank(environment.getProperty("DATABASE_URL"), safeGetenv("DATABASE_URL"));
        String fromSpringUrl =
                firstNonBlank(environment.getProperty("SPRING_DATASOURCE_URL"), safeGetenv("SPRING_DATASOURCE_URL"));

        String databaseUrl = firstNonBlank(fromDbUrl, fromSpringUrl);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        databaseUrl = databaseUrl.trim();

        // 仅配置了 SPRING_DATASOURCE_URL 且为 MySQL JDBC 时不覆盖（本地/自建 MySQL）
        if (fromDbUrl == null && databaseUrl.startsWith("jdbc:mysql:")) {
            return;
        }

        if (databaseUrl.startsWith("jdbc:postgresql:")) {
            putPostgresDatasource(environment, appendSslModeIfNeeded(databaseUrl));
            return;
        }

        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            Parsed parsed = parsePostgresUri(databaseUrl);
            putPostgresDatasource(
                    environment, appendSslModeIfNeeded(parsed.jdbcUrl()), parsed.username(), parsed.password());
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private static String safeGetenv(String key) {
        try {
            return System.getenv(key);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    /** Render Managed Postgres 常需 SSL；若 URL 尚无 sslmode 则补上（避免静默连不上）。 */
    private static String appendSslModeIfNeeded(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return jdbcUrl;
        }
        String lower = jdbcUrl.toLowerCase(Locale.ROOT);
        if (lower.contains("sslmode=")) {
            return jdbcUrl;
        }
        try {
            String afterScheme = jdbcUrl.substring("jdbc:postgresql://".length());
            int slash = afterScheme.indexOf('/');
            if (slash < 0 || slash >= afterScheme.length() - 1) {
                return jdbcUrl;
            }
            String hostPort = afterScheme.substring(0, slash);
            if (!hostPort.toLowerCase(Locale.ROOT).contains("render.com")) {
                return jdbcUrl;
            }
        } catch (RuntimeException ignored) {
            return jdbcUrl;
        }
        char sep = jdbcUrl.contains("?") ? '&' : '?';
        return jdbcUrl + sep + "sslmode=require";
    }

    private static void putPostgresDatasource(ConfigurableEnvironment environment, String jdbcUrl) {
        putPostgresDatasource(environment, jdbcUrl, null, null);
    }

    private static void putPostgresDatasource(
            ConfigurableEnvironment environment, String jdbcUrl, String username, String password) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("spring.datasource.url", jdbcUrl);
        if (username != null && !username.isBlank()) {
            map.put("spring.datasource.username", username);
        }
        if (password != null && !password.isBlank()) {
            map.put("spring.datasource.password", password);
        }
        map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        map.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        map.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, map));
    }

    private record Parsed(String jdbcUrl, String username, String password) {}

    private static Parsed parsePostgresUri(String raw) {
        String surrogate = raw.replaceFirst("^postgres(ql)?://", "http://");
        URI uri = URI.create(surrogate);

        String userInfo = uri.getRawUserInfo();
        String username = null;
        String password = null;
        if (userInfo != null && !userInfo.isEmpty()) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = urlDecode(userInfo.substring(0, colon));
                password = urlDecode(userInfo.substring(colon + 1));
            } else {
                username = urlDecode(userInfo);
            }
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("DATABASE_URL is missing host");
        }
        int port = uri.getPort();
        String path = uri.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            throw new IllegalArgumentException("DATABASE_URL is missing database name in path");
        }
        String database = path.startsWith("/") ? path.substring(1) : path;

        StringBuilder jdbc = new StringBuilder();
        jdbc.append("jdbc:postgresql://").append(host);
        if (port > 0) {
            jdbc.append(':').append(port);
        }
        jdbc.append('/').append(database);
        String query = uri.getRawQuery();
        if (query != null && !query.isEmpty()) {
            jdbc.append('?').append(query);
        }
        return new Parsed(jdbc.toString(), username, password);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
