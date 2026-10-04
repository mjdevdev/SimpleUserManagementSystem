package com.mjdev.SimpleUserManagement.model;

/**
 * Form payload of POST /register. The oauth* fields arrive as hidden inputs
 * when the registration was started from a Google callback; blank for local.
 */
// Naming: this record is bound from HTTP form fields, not from SQL, so the
// camelCase -> snake_case mapping (see User.java) does not apply here — the
// parameter names in the HTML forms must match these component names instead.
public record RegisterForm(
        String username,
        String nickname,
        String email,
        String password,
        String oauthProvider,
        String oauthHandle,
        String oauthAvatar
) {
}
