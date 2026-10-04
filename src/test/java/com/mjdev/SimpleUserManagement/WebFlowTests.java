package com.mjdev.SimpleUserManagement;

import com.mjdev.SimpleUserManagement.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.RequestDispatcher;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 1/2/3 + logoff & error page flows: public pages, security redirects,
 * local register/login/logoff, OAuth2 registration, timestamps, error page.
 */
class WebFlowTests extends BaseIntegrationTest {

    // ---------- Part 1: pages and security routing ----------

    @Test
    void publicPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Simple User Management")));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Log in")))
                .andExpect(content().string(containsString("Continue with Google")));
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create an account")));
    }

    @Test
    void protectedPagesRedirectAnonymousUsers() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    // ---------- Part 2: local register / login / logoff ----------

    @Test
    void fullLocalAuthFlow() throws Exception {
        String username = uniqueUsername();
        registerUser(username);

        MvcResult loginResult = login(username, "password123");

        MvcResult me = mockMvc.perform(get("/me").session((MockHttpSession) loginResult.getRequest().getSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(username)))
                .andExpect(content().string(containsString("Nick " + username)))
                .andReturn();

        logout(me); // asserts redirect to /login?logout

        mockMvc.perform(get("/me").session((MockHttpSession) me.getRequest().getSession()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void registerRejectsDuplicatesAndShortPasswords() throws Exception {
        String username = uniqueUsername();
        registerUser(username);

        MvcResult page = getPage("/register");
        mockMvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", username)
                        .param("password", "password123")
                        .param("_csrf", csrf(page)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already taken")));

        page = getPage("/register");
        mockMvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", uniqueUsername())
                        .param("password", "short")
                        .param("_csrf", csrf(page)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("at least 8")));
    }

    @Test
    void wrongPasswordRedirectsBackWithError() throws Exception {
        String username = uniqueUsername();
        registerUser(username);

        MvcResult page = getPage("/login");
        mockMvc.perform(post("/login")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", username)
                        .param("password", "wrong-password")
                        .param("_csrf", csrf(page)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ---------- Part 2/4: last_login and last_logoff ----------

    @Test
    void lastLogoffIsStampedOnlyOnLogout() throws Exception {
        String username = uniqueUsername();
        registerUser(username);

        MvcResult loginResult = login(username, "password123");
        User afterLogin = userRepository.findByUsername(username).orElseThrow();
        assertNotNull(afterLogin.lastLogin(), "last_login must be set on login");
        assertNull(afterLogin.lastLogoff(), "last_logoff must stay empty until logoff");

        MvcResult me = mockMvc.perform(get("/me").session((MockHttpSession) loginResult.getRequest().getSession()))
                .andExpect(status().isOk())
                .andReturn();
        logout(me);

        User afterLogout = userRepository.findByUsername(username).orElseThrow();
        assertNotNull(afterLogout.lastLogoff(), "last_logoff must be stamped on logoff");
    }

    // ---------- Part 3: OAuth2 registration flow ----------

    @Test
    void googleEntryRedirectsToGoogleConsent() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")))
                .andExpect(header().string("Location", containsString("login/oauth2/code/google")));
    }

    @Test
    void oauthRegistrationCreatesUserWithEmptyPassword() throws Exception {
        String handle = "junit-sub-" + UUID.randomUUID().toString().substring(0, 8);
        String email = uniqueUsername() + "@gmail.test";

        MvcResult page = getPage("/register");
        mockMvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("oauthProvider", "google")
                        .param("oauthHandle", handle)
                        .param("email", email)
                        .param("nickname", "JUnit Google User")
                        .param("oauthAvatar", "http://example.test/avatar.png")
                        .param("_csrf", csrf(page)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        User user = userRepository.findByUsername(email).orElseThrow();
        assertEquals("google", user.oauth2Provider());
        assertEquals(handle, user.oauth2Handle());
        assertNull(user.password(), "OAuth2 users must have no password");
        assertEquals("http://example.test/avatar.png", user.avatarUrl());

        // the same Google account (handle) cannot register twice
        page = getPage("/register");
        mockMvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("oauthProvider", "google")
                        .param("oauthHandle", handle)
                        .param("email", "other-" + email)
                        .param("_csrf", csrf(page)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already registered")));

        // an email already taken by another user is rejected too
        page = getPage("/register");
        mockMvc.perform(post("/register")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("oauthProvider", "google")
                        .param("oauthHandle", handle + "-x")
                        .param("email", email)
                        .param("_csrf", csrf(page)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("already exists")));
    }

    // ---------- Error page ----------

    @Test
    void errorPageShownFor404() throws Exception {
        // anonymous user on a public path with no resource -> 404
        mockMvc.perform(get("/css/missing.css").accept(TEXT_HTML))
                .andExpect(status().isNotFound());

        // authenticated user on an unknown URL -> 404
        String username = uniqueUsername();
        registerUser(username);
        MvcResult loginResult = login(username, "password123");
        mockMvc.perform(get("/no-such-page")
                        .session((MockHttpSession) loginResult.getRequest().getSession())
                        .accept(TEXT_HTML))
                .andExpect(status().isNotFound());

        // the styled error page is rendered by the /error handler
        // (MockMvc cannot follow the container's ERROR dispatch, so the
        // rendering is asserted directly against /error)
        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Not Found")
                        .accept(TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
    }
}
