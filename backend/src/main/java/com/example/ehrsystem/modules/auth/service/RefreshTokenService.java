package com.example.ehrsystem.modules.auth.service;

import com.example.ehrsystem.modules.auth.entity.RefreshToken;
import com.example.ehrsystem.modules.auth.repository.RefreshTokenRepository;
import com.example.ehrsystem.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public String create(User user, String ipAddress, String userAgent) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration));

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RefreshToken verify(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new IllegalArgumentException("Token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        return refreshToken;
    }

    @Transactional
    public String rotate(RefreshToken refreshToken, String ipAddress, String userAgent) {
        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        User user = new User();
        user.setId(refreshToken.getUserId());

        return create(user, ipAddress, userAgent);
    }

    @Transactional
    public void revokeOne(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAll(Long userId) {
        var tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
        }
        refreshTokenRepository.saveAll(tokens);
    }

    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiresAtBeforeAndRevokedTrue(LocalDateTime.now());
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        deleteExpiredTokens();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}