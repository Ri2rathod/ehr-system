package com.example.ehrsystem.modules.auth.security;

import com.example.ehrsystem.modules.user.entity.User;
import com.example.ehrsystem.modules.role.repository.RolePermissionRepository;
import com.example.ehrsystem.modules.user.repository.UserPermissionOverrideRepository;
import com.example.ehrsystem.modules.permission.entity.Permission;
import com.example.ehrsystem.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.example.ehrsystem.modules.role.entity.Role;
import com.example.ehrsystem.modules.role.entity.RolePermission;
import com.example.ehrsystem.modules.user.entity.Effect;
import com.example.ehrsystem.modules.user.entity.UserPermissionOverride;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        if (Boolean.TRUE.equals(user.getIsAccountLocked())) {
            throw new LockedException("User account is locked");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new DisabledException("User account is inactive");
        }

        // Assemble authorities from roles, permissions, and overrides
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        // Role authorities
        user.getRoles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.getName())));
        // Permission authorities via role permissions
        List<Long> roleIds = user.getRoles().stream().map(Role::getId).collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            List<RolePermission> rpList = rolePermissionRepository.findByRoleIdIn(roleIds);
            rpList.forEach(rp -> authorities.add(new SimpleGrantedAuthority("PERM_" + rp.getPermission().getCode())));
        }
        // User-specific overrides (ALLOW only for now)
        List<UserPermissionOverride> overrides = userPermissionOverrideRepository.findByUserIdAndIsActiveTrue(user.getId());
        overrides.forEach(o -> {
            if (o.getEffect() == Effect.ALLOW) {
                authorities.add(new SimpleGrantedAuthority("PERM_" + o.getPermission().getCode()));
            }
        });
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountLocked(false)
                .disabled(false)
                .build();
    }
}