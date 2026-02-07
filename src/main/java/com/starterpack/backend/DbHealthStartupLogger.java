package com.starterpack.backend;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbHealthStartupLogger {
    private static final Logger logger = LoggerFactory.getLogger(DbHealthStartupLogger.class);

    @Bean
    public ApplicationRunner dbHealthOnStartup(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                logger.info("DB health check OK");
            } catch (Exception ex) {
                logger.warn("DB health check FAILED: {}", ex.getMessage());
            }
        };
    }
}
