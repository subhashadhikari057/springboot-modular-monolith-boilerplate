package com.starterpack.backend.modules.users.infrastructure;

import java.util.List;
import java.util.Optional;

import com.starterpack.backend.modules.users.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findAll();

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findById(Integer id);
}
