package com.example.ehrsystem.modules.user.repository;

import com.example.ehrsystem.modules.user.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, Long> {
    List<UserPermissionOverride> findByUserIdAndIsActiveTrue(Long userId);
}
