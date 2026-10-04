package com.mjdev.SimpleUserManagement.model;

import java.time.LocalDateTime;

/**
 * Row of the `users` table. Password is null/empty for OAuth2-only users.
 */
// Naming: the JDBC row mapper (DataClassRowMapper in UserRepository) maps
// camelCase record components to snake_case SQL columns automatically,
// e.g. avatarUrl -> avatar_url, lastLogin -> last_login, oauth2Provider -> oauth2_provider.
// Renaming a column in schema.sql therefore requires renaming the matching field here (and vice versa).
public record User(
        String id,
        String username,
        String password,
        String nickname,
        String email,
        String avatarUrl,
        String role,
        boolean enabled,
        boolean admin,
        String oauth2Provider,
        String oauth2Handle,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        LocalDateTime lastLogoff
) {
}
