package com.example.ehrsystem.modules.auth.service;


import com.example.ehrsystem.modules.auth.dto.request.LoginRequest;
import com.example.ehrsystem.modules.auth.dto.request.RegisterRequest;
import com.example.ehrsystem.modules.auth.dto.response.AuthResponse;
import com.example.ehrsystem.modules.role.entity.Role;
import com.example.ehrsystem.modules.role.service.RoleService;
import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && userService.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        String roleName = (request.getRoleName() == null || request.getRoleName().isBlank())
                ? "PATIENT"
                : request.getRoleName().trim().toUpperCase();

        Role role = roleService.getByName(roleName);

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .username(request.getUsername() != null && !request.getUsername().isBlank()
                        ? request.getUsername().trim()
                        : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneCountryCode(request.getPhoneCountryCode())
                .phoneNumber(request.getPhoneNumber())
                .displayName(buildDisplayName(
                        request.getFirstName(),
                        request.getMiddleName(),
                        request.getLastName()
                ))
                .roles(Set.of(role))
                .build();

        User savedUser = userService.save(user);

        return buildAuthResponse(savedUser, null);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userService.getByEmail(request.getEmail().trim().toLowerCase());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("User account is inactive");
        }

        if (Boolean.TRUE.equals(user.getIsAccountLocked())) {
            throw new IllegalArgumentException("User account is locked");
        }

        return buildAuthResponse(user, null);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .userUuid(user.getUuid())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getDisplayName())
                .roles(user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private String buildDisplayName(String firstName, String middleName, String lastName) {
        StringBuilder fullName = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            fullName.append(firstName.trim());
        }

        if (middleName != null && !middleName.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(middleName.trim());
        }

        if (lastName != null && !lastName.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(lastName.trim());
        }

        return fullName.toString();
    }
}