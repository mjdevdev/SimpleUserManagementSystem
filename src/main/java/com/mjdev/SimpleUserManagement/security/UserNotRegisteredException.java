package com.mjdev.SimpleUserManagement.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Thrown when an OAuth2 callback belongs to a handle that has no row in the
 * users table yet; the failure handler turns this into a redirect to /register.
 */
public class UserNotRegisteredException extends AuthenticationException {

    private final OidcUser oidcUser;

    public UserNotRegisteredException(OidcUser oidcUser) {
        super("user_not_registered");
        this.oidcUser = oidcUser;
    }

    public OidcUser getOidcUser() {
        return oidcUser;
    }
}
