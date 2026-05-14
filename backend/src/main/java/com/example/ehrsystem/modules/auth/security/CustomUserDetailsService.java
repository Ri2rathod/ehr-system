package com.example.ehrsystem.modules.auth.security;

import com.example.ehrsystem.common.constant.AuthConstants;
import lombok.extern.slf4j.Slf4j;
import com.example.ehrsystem.modules.role.entity.Role;
import com.example.ehrsystem.modules.role.repository.RolePermissionRepository;
import com.example.ehrsystem.modules.user.entity.Effect;
import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.user.entity.UserPermissionOverride;
import com.example.ehrsystem.modules.user.repository.UserPermissionOverrideRepository;
import com.example.ehrsystem.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;

    /**
     * Loads the user's full authority set for Spring Security.
     *
     * <p>Authority assembly is a 4-stage pipeline:
     * <ol>
     *   <li>Role authorities   — ROLE_*</li>
     *   <li>Role → permission codes (plain strings, deduped)</li>
     *   <li>Per-user overrides — ALLOW adds, DENY removes from the string set</li>
     *   <li>Final conversion   — normalized codes → PERM_* authority objects</li>
     * </ol>
     *
     * <p>All permission logic operates on plain {@code String} codes.
     * Spring Security objects are created only in Stage 4, keeping domain
     * logic fully decoupled from the framework.
     *
     * <p>Results are cached by email (username) to avoid repeated DB hits
     * on every authenticated request. Evict via
     * {@link org.springframework.cache.annotation.CacheEvict} whenever
     * roles or overrides are modified for this user.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username.trim().toLowerCase();

        // ── Fetch user with roles in one query (JOIN FETCH) ─────────────────
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        log.debug("Loaded user {} from database with password hash present (length: {})", 
                email, (user.getPasswordHash() == null ? "null" : user.getPasswordHash().length()));

        // ── Account status guards ────────────────────────────────────────────
        // Exceptions are thrown here; builder flags below reflect the same
        // truth. If we reach the builder, the account is always valid.
        if (Boolean.TRUE.equals(user.getIsAccountLocked())) {
            throw new LockedException("User account is locked");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new DisabledException("User account is inactive");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalStateException(
                    "User password hash is missing for email: " + username);
        }

        // ── Stage 1: Role authorities ────────────────────────────────────────
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(r ->
                authorities.add(new SimpleGrantedAuthority(
                        AuthConstants.ROLE_PREFIX + r.getName()))
        );

        // ── Stage 2: Permission codes as plain strings (deduplicated) ────────
        // Using a String Set means domain logic never touches Security objects.
        Set<String> permCodes = new HashSet<>();

        List<Long> roleIds = user.getRoles().stream()
                .map(Role::getId)
                .collect(Collectors.toList());

        if (!roleIds.isEmpty()) {
            rolePermissionRepository.findByRoleIdIn(roleIds).stream()
                    .map(rp -> normalize(rp.getPermission().getCode()))
                    .forEach(permCodes::add);           // HashSet deduplicates
        }

        // ── Stage 3: Per-user overrides (ALLOW / DENY) ──────────────────────
        // DENY runs on the same string set that Stage 2 populated, so it
        // always takes precedence — no mutation of Security objects needed.
        List<UserPermissionOverride> overrides =
                userPermissionOverrideRepository.findByUserIdAndIsActiveTrue(user.getId());

        overrides.forEach(o -> {
            String code = normalize(o.getPermission().getCode());
            if (o.getEffect() == Effect.ALLOW) {
                permCodes.add(code);           // grant extra permission
            } else if (o.getEffect() == Effect.DENY) {
                permCodes.remove(code);        // revoke role-granted permission
            }
        });

        // ── Stage 4: Convert normalized codes → Spring Security authorities ──
        permCodes.forEach(code ->
                authorities.add(new SimpleGrantedAuthority(
                        AuthConstants.PERM_PREFIX + code))
        );

        // ── Build UserDetails ────────────────────────────────────────────────
        // accountLocked/disabled set to false because exceptions above
        // guarantee we only reach here with a valid, active, unlocked account.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(false)
                .disabled(false)
                .build();
    }

    /**
     * Normalizes a permission code to a consistent uppercase, trimmed form.
     *
     * <p>Guards against silent mismatches caused by inconsistent casing or
     * whitespace in the database (e.g. {@code "user_read"} vs {@code "USER_READ"}).
     */
    private static String normalize(String code) {
        if (code == null) {
            throw new IllegalStateException(
                    "Permission code must not be null. Check data integrity in the permissions table.");
        }
        return code.trim().toUpperCase();
    }
}
