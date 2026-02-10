package com.starterpack.backend.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.Session;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionRepository sessionRepository;
    private final AuthProperties authProperties;

    public SessionAuthenticationFilter(SessionRepository sessionRepository, AuthProperties authProperties) {
        this.sessionRepository = sessionRepository;
        this.authProperties = authProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = extractSessionToken(request);
            if (token != null) {
                sessionRepository.findByTokenAndExpiresAtAfter(token, OffsetDateTime.now())
                        .ifPresent(session -> authenticate(session, request));
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Session session, HttpServletRequest request) {
        User user = session.getUser();
        List<GrantedAuthority> authorities = new ArrayList<>();
        Role role = user.getRole();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractSessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        String cookieName = authProperties.getCookie().getName();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
