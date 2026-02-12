package com.starterpack.backend.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.starterpack.backend.modules.auth.application.model.CachedAuthContext;
import com.starterpack.backend.modules.auth.application.port.AuthSessionCachePort;
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
    private final AuthSessionCachePort authSessionCache;

    public SessionAuthenticationFilter(
            SessionRepository sessionRepository,
            AuthProperties authProperties,
            AuthSessionCachePort authSessionCache
    ) {
        this.sessionRepository = sessionRepository;
        this.authProperties = authProperties;
        this.authSessionCache = authSessionCache;
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
                authSessionCache.findBySessionToken(token)
                        .ifPresentOrElse(
                                cached -> authenticate(cached, request),
                                () -> sessionRepository.findByTokenAndExpiresAtAfter(token, OffsetDateTime.now())
                                        .ifPresent(session -> {
                                            authSessionCache.cacheSession(session);
                                            authenticate(session, request);
                                        })
                        );
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Session session, HttpServletRequest request) {
        User user = session.getUser();
        List<GrantedAuthority> authorities = authorities(user.getRole());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticate(CachedAuthContext context, HttpServletRequest request) {
        User principal = new User();
        principal.setId(context.userId());
        Role role = new Role();
        role.setName(context.roleName());
        principal.setRole(role);

        List<GrantedAuthority> authorities = authorities(context.roleName(), context.permissions());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(request);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private List<GrantedAuthority> authorities(Role role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
        }
        return authorities;
    }

    private List<GrantedAuthority> authorities(String roleName, Set<String> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roleName != null && !roleName.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        }
        if (permissions != null) {
            permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        }
        return authorities;
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
