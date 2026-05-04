package com.example.ehrsystem.common.constant;

/**
 * Central contract for Spring Security authority string prefixes.
 * <p>
 * All authority names MUST be built using these constants.
 * Never hardcode "ROLE_" or "PERM_" anywhere else.
 */
public final class AuthConstants {

    private AuthConstants() {
        // utility class — no instances
    }

    /** Prefix for role-based authorities.  Example: {@code ROLE_ADMIN} */
    public static final String ROLE_PREFIX = "ROLE_";

    /** Prefix for permission-based authorities. Example: {@code PERM_USER_READ} */
    public static final String PERM_PREFIX = "PERM_";

    /** Cache name used by {@code CustomUserDetailsService}. */
    public static final String CACHE_USER_DETAILS = "userDetails";
}
