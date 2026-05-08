package com.testplatform.env;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render 等 PaaS 会注入 {@code DATABASE_URL}（{@code postgres://...}），而 Spring 需要
 * {@code jdbc:postgresql://...}。在 {@code RENDER=true} 时把连接信息映射到标准数据源属性。
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class RenderDatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE_NAME = "renderDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!"true".equalsIgnoreCase(environment.getProperty("RENDER"))) {
            return;
        }
        if (hasExplicitSpringDatasourceUrl(environment)) {
            return;
        }
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        if (databaseUrl.startsWith("jdbc:")) {
            putPostgresDatasource(environment, databaseUrl, null, null);
            return;
        }
        Parsed parsed = parsePostgresUri(databaseUrl);
        putPostgresDatasource(environment, parsed.jdbcUrl(), parsed.username(), parsed.password());
    }

    private static boolean hasExplicitSpringDatasourceUrl(ConfigurableEnvironment environment) {
        String v = environment.getProperty("SPRING_DATASOURCE_URL");
        return v != null && !v.isBlank();
    }

    private static void putPostgresDatasource(
            ConfigurableEnvironment environment,
            String jdbcUrl,
            String username,
            String password) {
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
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, map));
    }

    private record Parsed(String jdbcUrl, String username, String password) {}

    /**
     * 将 {@code postgres://} / {@code postgresql://} 解析为 JDBC URL 与账号（密码可含特殊字符）。
     */
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
