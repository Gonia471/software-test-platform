package com.testplatform.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 解析 Render/Heroku 风格的 {@code DATABASE_URL}（{@code postgresql://...}）为 JDBC/Hikari 参数。
 */
final class PostgresDeploymentUrlSupport {

    private PostgresDeploymentUrlSupport() {}

    record JdbcParts(String jdbcUrl, String username, String password) {}

    static String appendSslModeIfNeeded(String jdbcUrl) {
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

    static JdbcParts toJdbcParts(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("database URL is blank");
        }
        String u = rawUrl.trim();
        if (u.startsWith("jdbc:postgresql:")) {
            return new JdbcParts(appendSslModeIfNeeded(u), null, null);
        }
        if (u.startsWith("postgres://") || u.startsWith("postgresql://")) {
            return fromPostgresUri(u);
        }
        throw new IllegalArgumentException("unsupported database URL scheme (expected postgresql:// or jdbc:postgresql:)");
    }

    private static JdbcParts fromPostgresUri(String raw) {
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
        return new JdbcParts(appendSslModeIfNeeded(jdbc.toString()), username, password);
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
