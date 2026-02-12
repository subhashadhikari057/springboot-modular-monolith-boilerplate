package com.starterpack.backend.modules.users.infrastructure;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.Session;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByToken(String token);

    Optional<Session> findByRefreshToken(String refreshToken);

    @EntityGraph(attributePaths = {"user", "user.role", "user.role.permissions"})
    Optional<Session> findByTokenAndExpiresAtAfter(String token, OffsetDateTime expiresAt);

    @EntityGraph(attributePaths = {"user", "user.role", "user.role.permissions"})
    Optional<Session> findByRefreshTokenAndRefreshExpiresAtAfter(String refreshToken, OffsetDateTime refreshExpiresAt);

    @EntityGraph(attributePaths = {"user", "user.role", "user.role.permissions"})
    @Query("select s from Session s where s.id = :id")
    Optional<Session> findByIdWithUserAndPermissions(UUID id);

    Optional<Session> findByIdAndUserId(UUID id, UUID userId);

    void deleteByToken(String token);

    void deleteByRefreshToken(String refreshToken);

    void deleteByUserId(UUID userId);

    void deleteByUserRoleId(Integer roleId);

    List<Session> findAllByUserId(UUID userId);
}
