package com.starterpack.backend.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi adminApiDocs() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/admin/**", "/api/uploads/**", "/api/mobile/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi usersApiDocs() {
        return GroupedOpenApi.builder()
                .group("mobile")
                .pathsToMatch("/api/mobile/**")
                .build();
    }
}
