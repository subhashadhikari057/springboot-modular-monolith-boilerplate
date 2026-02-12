package com.starterpack.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    private final Users users = new Users();

    public Users getUsers() {
        return users;
    }

    public static class Users {
        private Duration listTtl = Duration.ofMinutes(5);

        public Duration getListTtl() {
            return listTtl;
        }

        public void setListTtl(Duration listTtl) {
            this.listTtl = listTtl;
        }
    }
}
