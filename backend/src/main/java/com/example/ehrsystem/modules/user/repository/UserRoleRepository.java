package com.example.ehrsystem.modules.user.repository;

import com.example.ehrsystem.modules.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    List<UserRole> findByUserIdAndIsActiveTrue(Long userId);
}
