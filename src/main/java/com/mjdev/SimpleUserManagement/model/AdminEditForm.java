package com.mjdev.SimpleUserManagement.model;

/**
 * Form payload of the admin user-edit screen. Password blank = keep unchanged.
 */
// Naming: bound from HTTP form fields, not SQL — the camelCase -> snake_case
// mapping (see User.java) does not apply; the edit form's input names must
// match these component names instead.
public record AdminEditForm(
        String username,
        String nickname,
        String email,
        String password
) {
}
