package com.mjdev.SimpleUserManagement.repository;

import com.mjdev.SimpleUserManagement.model.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        return jdbc.sql("SELECT * FROM users WHERE username = :username")
                .param("username", username)
                .query(User.class)
                .optional();
    }

    public Optional<User> findById(String id) {
        return jdbc.sql("SELECT * FROM users WHERE id = :id")
                .param("id", id)
                .query(User.class)
                .optional();
    }

    public java.util.List<User> findAll() {
        return jdbc.sql("SELECT * FROM users ORDER BY created_at")
                .query(User.class)
                .list();
    }

    public Optional<User> findByProviderAndHandle(String provider, String handle) {
        return jdbc.sql("SELECT * FROM users WHERE oauth2_provider = :provider AND oauth2_handle = :handle")
                .param("provider", provider)
                .param("handle", handle)
                .query(User.class)
                .optional();
    }

    public void insert(User u) {
        jdbc.sql("""
                INSERT INTO users (id, username, password, nickname, email, avatar_url,
                                   role, enabled, `admin`, oauth2_provider, oauth2_handle,
                                   created_at, last_login, last_logoff)
                VALUES (:id, :username, :password, :nickname, :email, :avatarUrl,
                        :role, :enabled, :admin, :oauth2Provider, :oauth2Handle,
                        NOW(), :lastLogin, :lastLogoff)
                """)
                .param("id", u.id())
                .param("username", u.username())
                .param("password", u.password())
                .param("nickname", u.nickname())
                .param("email", u.email())
                .param("avatarUrl", u.avatarUrl())
                .param("role", u.role())
                .param("enabled", u.enabled())
                .param("admin", u.admin())
                .param("oauth2Provider", u.oauth2Provider())
                .param("oauth2Handle", u.oauth2Handle())
                .param("lastLogin", u.lastLogin())
                .param("lastLogoff", u.lastLogoff())
                .update();
    }

    public void touchLastLogin(String username) {
        jdbc.sql("UPDATE users SET last_login = NOW() WHERE username = :username")
                .param("username", username)
                .update();
    }

    public void touchLastLogoff(String username) {
        jdbc.sql("UPDATE users SET last_logoff = NOW() WHERE username = :username")
                .param("username", username)
                .update();
    }

    /**
     * Admin edit: passwordHash null keeps the stored password unchanged.
     */
    public void updateProfile(String id, String username, String nickname, String email, String passwordHash) {
        jdbc.sql("""
                UPDATE users SET username = :username, nickname = :nickname, email = :email,
                                 password = COALESCE(:passwordHash, password)
                WHERE id = :id
                """)
                .param("id", id)
                .param("username", username)
                .param("nickname", nickname)
                .param("email", email)
                .param("passwordHash", passwordHash)
                .update();
    }

    public void deleteById(String id) {
        jdbc.sql("DELETE FROM users WHERE id = :id")
                .param("id", id)
                .update();
    }
}
