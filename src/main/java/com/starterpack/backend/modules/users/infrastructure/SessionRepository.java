package com.starterpack.backend.modules.users.infrastructure;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.Session;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByToken(String token);

    @EntityGraph(attributePaths = {"user", "user.role", "user.role.permissions"})
    Optional<Session> findByTokenAndExpiresAtAfter(String token, OffsetDateTime expiresAt);

    @EntityGraph(attributePaths = {"user", "user.role", "user.role.permissions"})
    Optional<Session> findByRefreshTokenAndRefreshExpiresAtAfter(String refreshToken, OffsetDateTime refreshExpiresAt);

    void deleteByToken(String token);

    void deleteByRefreshToken(String refreshToken);

    void deleteByUserId(UUID userId);
}
