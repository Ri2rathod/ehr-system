package com.example.ehrsystem.modules.user.service;

import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User getById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public User getByIdWithRoles(Long id) {
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public User getByUuid(UUID uuid) {
        return userRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User not found with uuid: " + uuid));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    public User getByEmailWithRoles(String email) {
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameAndDeletedAtIsNull(username);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLoginAudit(Long userId, LocalDateTime lastLoginAt, String lastLoginIp) {
        int updatedRows = userRepository.updateLastLoginAudit(userId, lastLoginAt, lastLoginIp);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }
    }
}
