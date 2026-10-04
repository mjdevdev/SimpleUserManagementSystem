package com.mjdev.SimpleUserManagement.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Wraps the Google-returned OidcUser but reports the local username as the
 * principal name, so controllers and the login handler can look the row up
 * in the users table by username.
 */
public class DbUserOidcUser implements OidcUser {

    private final OidcUser delegate;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public DbUserOidcUser(OidcUser delegate, String username) {
        this(delegate, username, delegate.getAuthorities());
    }

    public DbUserOidcUser(OidcUser delegate, String username,
                          Collection<? extends GrantedAuthority> authorities) {
        this.delegate = delegate;
        this.username = username;
        this.authorities = List.copyOf(authorities);
    }

    public static DbUserOidcUser withAdminRole(OidcUser delegate, String username) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(delegate.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new DbUserOidcUser(delegate, username, authorities);
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }
}
