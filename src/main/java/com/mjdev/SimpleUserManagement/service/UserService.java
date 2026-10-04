package com.mjdev.SimpleUserManagement.service;

import com.mjdev.SimpleUserManagement.model.AdminEditForm;
import com.mjdev.SimpleUserManagement.model.RegisterForm;
import com.mjdev.SimpleUserManagement.model.User;
import com.mjdev.SimpleUserManagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterForm form) {
        if (form.oauthProvider() != null && !form.oauthProvider().isBlank()) {
            registerOAuth(form);
        } else {
            registerLocal(form);
        }
    }

    private void registerLocal(RegisterForm form) {
        String username = form.username() == null ? "" : form.username().trim();
        String password = form.password() == null ? "" : form.password();

        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be 3-50 characters.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        String nickname = nicknameOr(form, username);
        String email = form.email() == null || form.email().isBlank()
                ? null
                : form.email().trim();

        userRepository.insert(new User(
                UUID.randomUUID().toString(),
                username,
                passwordEncoder.encode(password),
                nickname,
                email,
                null,          // avatar_url
                "USER",        // role
                true,          // enabled
                false,         // admin
                null,          // oauth2_provider (local user)
                null,          // oauth2_handle
                null,          // created_at set by DB
                null,          // last_login set by success handler
                null           // last_logoff set by logoff handler
        ));
    }

    private void registerOAuth(RegisterForm form) {
        String email = form.email() == null ? "" : form.email().trim();
        String handle = form.oauthHandle() == null ? "" : form.oauthHandle().trim();

        if (email.isBlank()) {
            throw new IllegalArgumentException("Email is required for " + form.oauthProvider() + " registration.");
        }
        if (handle.isBlank()) {
            throw new IllegalArgumentException("Missing " + form.oauthProvider() + " account handle.");
        }
        if (userRepository.findByUsername(email).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists. Please log in instead.");
        }
        if (userRepository.findByProviderAndHandle(form.oauthProvider(), handle).isPresent()) {
            throw new IllegalArgumentException("This " + form.oauthProvider() + " account is already registered. Please log in.");
        }

        String nickname = nicknameOr(form, email);

        userRepository.insert(new User(
                UUID.randomUUID().toString(),
                email,                            // username = Google email
                null,                             // password left empty for OAuth2 users
                nickname,
                email,
                form.oauthAvatar(),
                "USER",
                true,
                false,
                form.oauthProvider(),
                handle,
                null,
                null,
                null
        ));
    }

    private String nicknameOr(RegisterForm form, String fallback) {
        return form.nickname() == null || form.nickname().isBlank()
                ? fallback
                : form.nickname().trim();
    }

    /**
     * Admin edit: username/nickname/email always updated; password only when
     * the form supplies a new one.
     */
    public void adminUpdateUser(String id, AdminEditForm form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String username = form.username() == null ? "" : form.username().trim();
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be 3-50 characters.");
        }
        userRepository.findByUsername(username)
                .filter(other -> !other.id().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Username is already taken.");
                });

        String passwordHash = null;
        if (form.password() != null && !form.password().isBlank()) {
            if (form.password().length() < 8) {
                throw new IllegalArgumentException("Password must be at least 8 characters.");
            }
            passwordHash = passwordEncoder.encode(form.password());
        }

        String nickname = form.nickname() == null || form.nickname().isBlank()
                ? null
                : form.nickname().trim();
        String email = form.email() == null || form.email().isBlank()
                ? null
                : form.email().trim();

        userRepository.updateProfile(id, username, nickname, email, passwordHash);
    }
}
