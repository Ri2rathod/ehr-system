package com.example.ehrsystem.modules.role.repository;

import com.example.ehrsystem.modules.role.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleIdIn(List<Long> roleIds);
}
