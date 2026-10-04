package com.mjdev.SimpleUserManagement.security;

import com.mjdev.SimpleUserManagement.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Loads the Google profile, then looks the (google, sub) handle up in the
 * users table. Known handle -> principal named after the local username;
 * unknown handle -> UserNotRegisteredException, which the failure handler
 * turns into a redirect to the register page.
 */
@Service
public class GoogleOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public GoogleOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String handle = oidcUser.getSubject();

        return userRepository.findByProviderAndHandle("google", handle)
                .map(user -> (OidcUser) (user.admin()
                        ? DbUserOidcUser.withAdminRole(oidcUser, user.username())
                        : new DbUserOidcUser(oidcUser, user.username())))
                .orElseThrow(() -> new UserNotRegisteredException(oidcUser));
    }
}
