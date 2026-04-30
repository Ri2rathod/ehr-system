package com.example.ehrsystem.modules.permission.repository;

import com.example.ehrsystem.modules.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCodeAndDeletedAtIsNull(String code);
    boolean existsByCodeAndDeletedAtIsNull(String code);
}
