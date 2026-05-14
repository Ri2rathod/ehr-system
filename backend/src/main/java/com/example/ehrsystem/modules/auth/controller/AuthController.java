package com.example.ehrsystem.modules.auth.controller;

import com.example.ehrsystem.modules.auth.dto.request.LoginRequest;
import com.example.ehrsystem.modules.auth.dto.request.RefreshTokenRequest;
import com.example.ehrsystem.modules.auth.dto.request.RegisterRequest;
import com.example.ehrsystem.modules.auth.dto.response.AuthResponse;
import com.example.ehrsystem.modules.role.entity.Role;
import com.example.ehrsystem.modules.auth.service.AuthService;
import com.example.ehrsystem.modules.auth.service.RefreshTokenService;
import com.example.ehrsystem.modules.user.dto.response.CurrentUserResponse;
import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr(), userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr(), userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader("User-Agent");
        var refreshToken = refreshTokenService.verify(request.getRefreshToken());

        String newRefreshToken = refreshTokenService.rotate(refreshToken, httpRequest.getRemoteAddr(), userAgent);

        var userDetails = authService.loadUserByUserId(refreshToken.getUserId());
        String accessToken = authService.generateAccessToken(userDetails);

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(900L)
                .refreshTokenExpiresIn(604800L)
                .tokenType("Bearer")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revokeOne(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = authService.getUserIdByEmail(userDetails.getUsername());
        refreshTokenService.revokeAll(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        User user = userService.getByEmailWithRoles(authentication.getName());
        CurrentUserResponse response = CurrentUserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .displayName(user.getDisplayName())
                .phoneCountryCode(user.getPhoneCountryCode())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isAccountLocked(user.getIsAccountLocked())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .lastLoginAt(user.getLastLoginAt())
                .build();
        return ResponseEntity.ok(response);
    }
}
