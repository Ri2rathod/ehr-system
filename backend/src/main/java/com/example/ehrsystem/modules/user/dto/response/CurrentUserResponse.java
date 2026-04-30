package com.example.ehrsystem.modules.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentUserResponse {

    private Long id;
    private UUID uuid;
    private String email;
    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String displayName;
    private String phoneCountryCode;
    private String phoneNumber;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean isAccountLocked;
    private Set<String> roles;
    private LocalDateTime lastLoginAt;
}
