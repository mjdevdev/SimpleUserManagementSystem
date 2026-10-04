package com.mjdev.SimpleUserManagement.model;

/**
 * Read-only projection of a user row for the admin dashboard, with dates
 * pre-formatted for display.
 */
// Naming: assembled manually in AdminController.toRow() from a mapped User,
// so no automatic camelCase -> snake_case column mapping applies here —
// only records handed directly to JdbcClient .query(Class) get that.
public record AdminUserRow(
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
        String createdAt,
        String lastLogin,
        String lastLogoff
) {
}
