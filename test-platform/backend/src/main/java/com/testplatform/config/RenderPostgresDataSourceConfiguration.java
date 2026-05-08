package com.testplatform.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Docker/Render 上若注入 {@code DATABASE_URL}（{@code postgresql://...}），则显式提供 PostgreSQL
 * {@link DataSource}。比仅改 Environment 更可靠（避免数据源仍按 YAML 占位符落成 MySQL）。
 */
@Configuration(proxyBeanMethods = false)
@Conditional(OnPostgresDeploymentUrlCondition.class)
public class RenderPostgresDataSourceConfiguration {

    private static final int POOL_MIN = 2;
    private static final int POOL_MAX = 10;

    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource postgresDataSourceFromDeploymentUrl(Environment env) {
        String raw = PostgresUrlResolver.rawUrl(env);
        PostgresDeploymentUrlSupport.JdbcParts parts =
                PostgresDeploymentUrlSupport.toJdbcParts(raw);

        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("hikari-postgres-deploy");
        ds.setJdbcUrl(parts.jdbcUrl());
        if (parts.username() != null && !parts.username().isBlank()) {
            ds.setUsername(parts.username());
        }
        if (parts.password() != null && !parts.password().isBlank()) {
            ds.setPassword(parts.password());
        }
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMinimumIdle(POOL_MIN);
        ds.setMaximumPoolSize(POOL_MAX);
        return ds;
    }
}
