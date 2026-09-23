package com.dashenbank.cms.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Explicit MySQL primary datasource. A second Oracle CBS DataSource exists; without
 * {@code @Primary}, Flyway/JPA would bind to the CBS pool.
 */
@Configuration
public class MysqlDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driverClassName}") String driver) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("cms-mysql");
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName(driver);
        return ds;
    }
}
