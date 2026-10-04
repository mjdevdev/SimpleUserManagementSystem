package com.mjdev.SimpleUserManagement.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sends unknown OAuth2 accounts to the register page pre-filled with the
 * profile Google handed back; everything else goes to the login page.
 */
@Component
public class OAuthRegistrationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (exception instanceof UserNotRegisteredException e && e.getOidcUser().getEmail() != null) {
            String redirect = UriComponentsBuilder.fromPath("/register")
                    .queryParam("oauth", "google")
                    .queryParam("handle", e.getOidcUser().getSubject())
                    .queryParam("email", e.getOidcUser().getEmail())
                    .queryParam("nickname", e.getOidcUser().getFullName())
                    .queryParam("avatar", e.getOidcUser().getPicture())
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
            response.sendRedirect(redirect);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
