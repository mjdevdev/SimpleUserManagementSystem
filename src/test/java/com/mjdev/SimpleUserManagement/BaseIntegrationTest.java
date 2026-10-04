package com.mjdev.SimpleUserManagement;

import com.mjdev.SimpleUserManagement.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for integration tests that exercise the real HTTP stack (security
 * filters included) against the configured MySQL database. All test users
 * are created with a "junit..." username and removed again after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected JdbcClient jdbc;

    @AfterEach
    void removeJunitUsers() {
        jdbc.sql("DELETE FROM users WHERE username LIKE 'junit%'").update();
    }

    protected String uniqueUsername() {
        return "junit" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Pulls the CSRF token out of a rendered form page. */
    protected String csrf(MvcResult page) throws Exception {
        Matcher matcher = CSRF.matcher(page.getResponse().getContentAsString());
        assertTrue(matcher.find(), "no CSRF token found in " + page.getRequest().getRequestURI());
        return matcher.group(1);
    }

    protected MvcResult getPage(String url) throws Exception {
        return mockMvc.perform(get(url)).andReturn();
    }

    /** Registers a local user through POST /register and asserts success. */
    protected String registerUser(String username) throws Exception {
        MvcResult page = getPage("/register");
        mockMvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", username)
                        .param("nickname", "Nick " + username)
                        .param("email", username + "@test.local")
                        .param("password", "password123")
                        .param("_csrf", csrf(page)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
        return username;
    }

    /** Form login on the main site; asserts the redirect to /me. */
    protected MvcResult login(String username, String password) throws Exception {
        MvcResult page = getPage("/login");
        return mockMvc.perform(post("/login")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", username)
                        .param("password", password)
                        .param("_csrf", csrf(page)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/me"))
                .andReturn();
    }

    /** Form login on the admin login screen; asserts the redirect to /admin. */
    protected MvcResult adminLogin(String username, String password) throws Exception {
        MvcResult page = getPage("/admin/login");
        return mockMvc.perform(post("/admin/login")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", username)
                        .param("password", password)
                        .param("_csrf", csrf(page)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andReturn();
    }

    /** Presses the logoff button; asserts the redirect to /login?logout. */
    protected MvcResult logout(MvcResult authenticatedPage) throws Exception {
        return mockMvc.perform(post("/logout")
                        .session((MockHttpSession) authenticatedPage.getRequest().getSession())
                        .param("_csrf", csrf(authenticatedPage)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andReturn();
    }

    /** Grants the admin flag directly in the database. */
    protected void makeAdmin(String username) {
        jdbc.sql("UPDATE users SET `admin` = 1 WHERE username = :u")
                .param("u", username)
                .update();
    }
}
