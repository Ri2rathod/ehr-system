package com.example.ehrsystem.modules.auth.service;

import com.example.ehrsystem.modules.auth.dto.request.LoginRequest;
import com.example.ehrsystem.modules.auth.dto.request.RegisterRequest;
import com.example.ehrsystem.modules.auth.dto.response.AuthResponse;
import com.example.ehrsystem.modules.auth.security.JwtService;
import com.example.ehrsystem.modules.role.entity.Role;
import com.example.ehrsystem.modules.role.service.RoleService;
import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && userService.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        Role role = roleService.getByName("PATIENT");

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

        String token = jwtService.generateToken(buildUserDetails(savedUser));

        return buildAuthResponse(savedUser, token);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String remoteAddress) {
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userService.getByEmail(email);

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(remoteAddress);
        userService.save(user);

        String token = jwtService.generateToken(buildUserDetails(user));

        return buildAuthResponse(user, token);
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(
                        user.getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                                .collect(Collectors.toSet())
                )
                .accountLocked(Boolean.TRUE.equals(user.getIsAccountLocked()))
                .disabled(Boolean.FALSE.equals(user.getIsActive()))
                .build();
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
