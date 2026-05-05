package com.example.ehrsystem.modules.role.repository;

import com.example.ehrsystem.modules.role.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.permission WHERE rp.role.id IN :roleIds")
    List<RolePermission> findByRoleIdIn(@Param("roleIds") List<Long> roleIds);
}
