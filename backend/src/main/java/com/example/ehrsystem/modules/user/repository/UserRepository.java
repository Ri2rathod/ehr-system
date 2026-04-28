package com.example.ehrsystem.modules.user.repository;

import com.example.ehrsystem.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public  interface UserRepository  extends JpaRepository<User,Integer>{
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByUuidAndDeletedAtIsNull(UUID uuid);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByUsernameAndDeletedAtIsNull(String username);
}
