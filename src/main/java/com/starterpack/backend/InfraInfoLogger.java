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
public class InfraInfoLogger {
    private static final Logger logger = LoggerFactory.getLogger(InfraInfoLogger.class);

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
            String appPort = System.getProperty("server.port", "8080");
            logger.info("App: http://localhost:{}", appPort);
            logger.info("Postgres: {}", envOrDefault("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5433/app"));
            logger.info("Redis: {}:{}", envOrDefault("SPRING_REDIS_HOST", "localhost"), envOrDefault("SPRING_REDIS_PORT", "6380"));
            logger.info(
                    "RabbitMQ: amqp://{}:{} (ui http://localhost:15672)",
                    envOrDefault("SPRING_RABBITMQ_HOST", "localhost"),
                    envOrDefault("SPRING_RABBITMQ_PORT", "5672")
            );
            logger.info(
                    "Mailpit: smtp://{}:{} (ui http://localhost:8025)",
                    envOrDefault("SPRING_MAIL_HOST", "localhost"),
                    envOrDefault("SPRING_MAIL_PORT", "1025")
            );
            logger.info("RedisInsight: http://localhost:8001");
        };
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
