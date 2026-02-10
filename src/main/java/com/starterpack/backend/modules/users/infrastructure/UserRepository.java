package com.starterpack.backend.modules.users.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.starterpack.backend.modules.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findById(UUID id);

    @EntityGraph(attributePaths = "role")
    List<User> findAll();

    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findByEmailIgnoreCase(String email);
}
