package com.mjdev.SimpleUserManagement.security;

import com.mjdev.SimpleUserManagement.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

/**
 * Records last_login on every successful login, then redirects to the
 * configured target (user area or admin dashboard).
 */
public class LastLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final String targetUrl;

    public LastLoginSuccessHandler(UserRepository userRepository, String targetUrl) {
        this.userRepository = userRepository;
        this.targetUrl = targetUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        userRepository.touchLastLogin(authentication.getName());
        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}
