package com.dashenbank.cms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Dedicated Oracle CBS datasource. Independent of MySQL, Flyway, and complaint tables.
 * Missing/unreachable CBS must not prevent CMS from starting.
 */
@Configuration
@EnableConfigurationProperties(CbsProperties.class)
public class CbsDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(CbsDataSourceConfig.class);

    public static final String CBS_DATA_SOURCE = "cbsDataSource";
    public static final String CBS_JDBC_TEMPLATE = "cbsJdbcTemplate";

    static final class CbsUrlPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return StringUtils.hasText(context.getEnvironment().getProperty("cbs.datasource.url"));
        }
    }

    @Bean(name = CBS_DATA_SOURCE)
    @Conditional(CbsUrlPresent.class)
    public DataSource cbsDataSource(CbsProperties properties) {
        int connectTimeoutMs = Math.max(properties.getConnectTimeoutMs(), 1000);
        int queryTimeoutMs = Math.max(properties.getQueryTimeoutSeconds(), 1) * 1000;
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("cbs-oracle");
        ds.setJdbcUrl(properties.getDatasource().getUrl());
        ds.setUsername(properties.getDatasource().getUsername());
        ds.setPassword(properties.getDatasource().getPassword());
        ds.setDriverClassName(properties.getDatasource().getDriverClassName());
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(0);
        ds.setConnectionTimeout(connectTimeoutMs);
        ds.setInitializationFailTimeout(-1);
        ds.setAutoCommit(true);
        ds.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(connectTimeoutMs));
        ds.addDataSourceProperty("oracle.jdbc.ReadTimeout", String.valueOf(queryTimeoutMs));
        log.info("CBS Oracle datasource enabled (credentials are not logged).");
        return ds;
    }

    @Bean(name = CBS_JDBC_TEMPLATE)
    @Conditional(CbsUrlPresent.class)
    public NamedParameterJdbcTemplate cbsJdbcTemplate(
            @Qualifier(CBS_DATA_SOURCE) DataSource cbsDataSource,
            CbsProperties properties) {
        JdbcTemplate jdbc = new JdbcTemplate(cbsDataSource);
        jdbc.setQueryTimeout(Math.max(properties.getQueryTimeoutSeconds(), 1));
        return new NamedParameterJdbcTemplate(jdbc);
    }
}
