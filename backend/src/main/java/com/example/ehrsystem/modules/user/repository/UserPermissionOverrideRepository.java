package com.example.ehrsystem.modules.user.repository;

import com.example.ehrsystem.modules.user.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, Long> {
    @Query("SELECT upo FROM UserPermissionOverride upo JOIN FETCH upo.permission WHERE upo.user.id = :userId AND upo.isActive = true")
    List<UserPermissionOverride> findByUserIdAndIsActiveTrue(@Param("userId") Long userId);
}
