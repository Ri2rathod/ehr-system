package com.example.ehrsystem.modules.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name="expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name="ip_address", length = 45)
    private String ipAddress;

    @Column(name="user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name="revoked", nullable = false)
    private Boolean revoked;

    @Column(name="revoked_at")
    private LocalDateTime revokedAt;
}
