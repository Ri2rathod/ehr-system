package com.example.ehrsystem.modules.auth.dto.response;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
    private String tokenType;

    private Long userId;
    private UUID userUuid;

    private String email;
    private String username;

    private String firstName;
    private String lastName;
    private String displayName;

    private Set<String> roles;

    private LocalDateTime lastLoginAt;
}