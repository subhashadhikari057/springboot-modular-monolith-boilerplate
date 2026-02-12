package com.starterpack.backend.config;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.starterpack.backend.modules.auth.application.model.CachedAuthContext;
import com.starterpack.backend.modules.auth.infrastructure.AuthSessionCache;
import com.starterpack.backend.modules.users.domain.Permission;
import com.starterpack.backend.modules.users.domain.Role;
import com.starterpack.backend.modules.users.domain.Session;
import com.starterpack.backend.modules.users.domain.User;
import com.starterpack.backend.modules.users.infrastructure.SessionRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AuthSessionCache authSessionCache;
    @Mock
    private FilterChain filterChain;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesFromRedisCacheWithoutDatabaseLookup() throws Exception {
        AuthProperties authProperties = new AuthProperties();
        SessionAuthenticationFilter filter = new SessionAuthenticationFilter(sessionRepository, authProperties, authSessionCache);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("sid", "token-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        CachedAuthContext cached = new CachedAuthContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "token-1",
                OffsetDateTime.now().plusMinutes(10),
                "rid-1",
                OffsetDateTime.now().plusDays(1),
                "ADMIN",
                Set.of("user:read")
        );
        when(authSessionCache.findBySessionToken("token-1")).thenReturn(java.util.Optional.of(cached));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN", "user:read");
        verify(sessionRepository, never()).findByTokenAndExpiresAtAfter(any(), any());
    }

    @Test
    void fallsBackToDatabaseAndRepopulatesCacheOnRedisMiss() throws Exception {
        AuthProperties authProperties = new AuthProperties();
        SessionAuthenticationFilter filter = new SessionAuthenticationFilter(sessionRepository, authProperties, authSessionCache);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("sid", "token-2"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        Permission permission = new Permission();
        permission.setName("upload:read");
        Role role = new Role();
        role.setName("USER");
        role.setPermissions(Set.of(permission));
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setToken("token-2");
        session.setRefreshToken("rid-2");
        session.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        session.setRefreshExpiresAt(OffsetDateTime.now().plusDays(7));

        when(authSessionCache.findBySessionToken("token-2")).thenReturn(java.util.Optional.empty());
        when(sessionRepository.findByTokenAndExpiresAtAfter(any(), any())).thenReturn(java.util.Optional.of(session));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(authSessionCache).cacheSession(session);
        verify(sessionRepository).findByTokenAndExpiresAtAfter(any(), any());
    }
}
