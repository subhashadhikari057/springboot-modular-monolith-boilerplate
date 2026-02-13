package com.starterpack.backend.config;

import com.starterpack.backend.modules.auth.application.port.AuthSessionCachePort;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionAuthenticationFilter sessionAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/api-docs",
                                "/api-docs/**",
                                "/openapi",
                                "/openapi/**",
                                "/webjars/**",
                                "/api/mobile/auth/login",
                                "/api/mobile/auth/register",
                                "/api/mobile/auth/refresh",
                                "/api/mobile/auth/verify/confirm",
                                "/api/mobile/auth/password/forgot",
                                "/api/mobile/auth/password/reset"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter(
            SessionRepository sessionRepository,
            AuthProperties authProperties,
            AuthSessionCachePort authSessionCache
    ) {
        return new SessionAuthenticationFilter(sessionRepository, authProperties, authSessionCache);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
