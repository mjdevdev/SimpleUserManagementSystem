package com.mjdev.SimpleUserManagement.security;

import com.mjdev.SimpleUserManagement.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * Stamps last_logoff on the user row when the logoff button is pressed.
 * Session invalidation itself stays with Spring Security's built-in handlers.
 * Works for both local users and OAuth2 users because both principals are
 * named after the local username.
 */
@Component
public class LastLogoffLogoutHandler implements LogoutHandler {

    private final UserRepository userRepository;

    public LastLogoffLogoutHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        if (authentication != null) {
            userRepository.touchLastLogoff(authentication.getName());
        }
    }
}
 