package com.starterpack.backend.config;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class InfraInfoLogger {
    private static final Logger logger = LoggerFactory.getLogger(InfraInfoLogger.class);

    @Bean
    public ApplicationRunner dbHealthOnStartup(
            DataSource dataSource,
            Environment environment
    ) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                logger.info("DB health check OK");
            } catch (Exception ex) {
                logger.warn("DB health check FAILED: {}", ex.getMessage());
            }

            String appHost = environment.getProperty("server.address", "localhost");
            String appPort = environment.getProperty("server.port", "8080");
            String rabbitHost = environment.getProperty("spring.rabbitmq.host", "localhost");
            String rabbitPort = environment.getProperty("spring.rabbitmq.port", "5672");
            String mailHost = environment.getProperty("spring.mail.host", "localhost");
            String mailPort = environment.getProperty("spring.mail.port", "1025");
            logger.info("App: http://{}:{}", appHost, appPort);
            logger.info("Postgres: {}", environment.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5433/app"));
            logger.info(
                    "Redis: {}:{}",
                    environment.getProperty("spring.data.redis.host", "localhost"),
                    environment.getProperty("spring.data.redis.port", "6380")
            );
            logger.info(
                    "RabbitMQ: amqp://{}:{} (ui {})",
                    rabbitHost,
                    rabbitPort,
                    "http://" + rabbitHost + ":15672"
            );
            logger.info(
                    "Mailpit: smtp://{}:{} (ui {})",
                    mailHost,
                    mailPort,
                    "http://" + mailHost + ":8025"
            );
            logger.info("RedisInsight: http://{}:8001", appHost);
            logger.info("API Docs (admin): http://{}:{}/api-docs/admin", appHost, appPort);
            logger.info("API Docs (mobile): http://{}:{}/api-docs/mobile", appHost, appPort);
        };
    }
}
