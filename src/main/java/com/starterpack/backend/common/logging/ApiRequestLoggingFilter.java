package com.starterpack.backend.common.logging;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger("API_REQUEST");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Instant startedAt = Instant.now();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
            String path = request.getRequestURI();
            String message = String.format(
                    "%s %s -> %d (%d ms)",
                    request.getMethod(),
                    path,
                    response.getStatus(),
                    elapsedMs
            );
            String coloredMessage = colorizeByStatus(message, response.getStatus());

            int status = response.getStatus();
            if (status >= 500) {
                logger.error(coloredMessage);
            } else if (status >= 400) {
                logger.warn(coloredMessage);
            } else {
                logger.info(coloredMessage);
            }
        }
    }

    private String colorizeByStatus(String message, int status) {
        AnsiColor color;
        if (status >= 500) {
            color = AnsiColor.RED;
        } else if (status >= 400) {
            color = AnsiColor.YELLOW;
        } else if (status >= 300) {
            color = AnsiColor.CYAN;
        } else {
            color = AnsiColor.GREEN;
        }

        return AnsiOutput.toString(color, message, AnsiColor.DEFAULT);
    }
}
