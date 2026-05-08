package com.testplatform.config;

import java.util.Locale;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 仅在存在 Postgres 形态的 {@code DATABASE_URL} / {@code SPRING_DATASOURCE_URL} 时启用，
 * 与本地默认 MySQL 配置互斥。
 */
class OnPostgresDeploymentUrlCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String u = PostgresUrlResolver.rawUrl(env);
        if (u == null || u.isBlank()) {
            return false;
        }
        u = u.trim();
        if (u.startsWith("jdbc:mysql:")) {
            return false;
        }
        return u.startsWith("jdbc:postgresql:")
                || u.toLowerCase(Locale.ROOT).startsWith("postgres://")
                || u.toLowerCase(Locale.ROOT).startsWith("postgresql://");
    }
}
