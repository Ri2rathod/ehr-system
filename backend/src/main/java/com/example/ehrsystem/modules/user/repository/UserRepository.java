package com.example.ehrsystem.modules.user.repository;

import com.example.ehrsystem.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public  interface UserRepository  extends JpaRepository<User,Integer>{
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    Optional<User> findByUuidAndDeletedAtIsNull(UUID uuid);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByUsernameAndDeletedAtIsNull(String username);
}
