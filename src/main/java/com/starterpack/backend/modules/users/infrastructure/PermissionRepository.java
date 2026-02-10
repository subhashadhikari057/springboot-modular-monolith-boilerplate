package com.starterpack.backend.modules.users.infrastructure;

import java.util.Optional;

import com.starterpack.backend.modules.users.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByNameIgnoreCase(String name);
}
