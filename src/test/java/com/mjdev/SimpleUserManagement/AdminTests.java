package com.mjdev.SimpleUserManagement;

import com.mjdev.SimpleUserManagement.model.User;
import org.junit.jupiter.api.Test;
import jakarta.servlet.RequestDispatcher;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 5 + delete guard: separate admin login, dashboard, user editing,
 * admin-deletion protection.
 */
class AdminTests extends BaseIntegrationTest {

    private MockHttpSession sessionOf(MvcResult loginResult) {
        return (MockHttpSession) loginResult.getRequest().getSession();
    }

    @Test
    void adminCanLoginOnSeparateScreenAndSeeDashboard() throws Exception {
        String admin = uniqueUsername();
        registerUser(admin);
        makeAdmin(admin);

        MvcResult loginResult = adminLogin(admin, "password123");

        mockMvc.perform(get("/admin").session(sessionOf(loginResult)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Admin dashboard")))
                .andExpect(content().string(containsString(admin)));
    }

    @Test
    void nonAdminGetsForbiddenOnAdminArea() throws Exception {
        String username = uniqueUsername();
        registerUser(username);

        MvcResult loginResult = login(username, "password123");

        mockMvc.perform(get("/admin").session(sessionOf(loginResult)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditUsernameNicknameEmailAndPassword() throws Exception {
        String admin = uniqueUsername();
        registerUser(admin);
        makeAdmin(admin);
        String target = uniqueUsername();
        registerUser(target);
        User targetUser = userRepository.findByUsername(target).orElseThrow();

        MvcResult loginResult = adminLogin(admin, "password123");
        MockHttpSession session = sessionOf(loginResult);

        MvcResult editPage = mockMvc.perform(get("/admin/users/" + targetUser.id()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit user")))
                .andReturn();

        mockMvc.perform(post("/admin/users/" + targetUser.id()).session(session)
                        .param("username", target)
                        .param("nickname", "Edited Nick")
                        .param("email", target + "@edited.test")
                        .param("password", "brand-new-pass")
                        .param("_csrf", csrf(editPage)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?saved"));

        User updated = userRepository.findByUsername(target).orElseThrow();
        assertEquals("Edited Nick", updated.nickname());
        assertEquals(target + "@edited.test", updated.email());

        // the new password must work on the main site, the old one must not
        login(target, "brand-new-pass");
        MvcResult page = getPage("/login");
        mockMvc.perform(post("/login")
                        .session((MockHttpSession) page.getRequest().getSession())
                        .param("username", target)
                        .param("password", "password123")
                        .param("_csrf", csrf(page)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void adminCannotDeleteOtherAdminsOrThemselves() throws Exception {
        String admin = uniqueUsername();
        registerUser(admin);
        makeAdmin(admin);
        User adminUser = userRepository.findByUsername(admin).orElseThrow();

        MvcResult loginResult = adminLogin(admin, "password123");
        MockHttpSession session = sessionOf(loginResult);
        MvcResult dash = mockMvc.perform(get("/admin").session(session)).andReturn();

        mockMvc.perform(post("/admin/users/" + adminUser.id() + "/delete")
                        .session(session).param("_csrf", csrf(dash)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?delete-denied"));

        assertTrue(userRepository.findByUsername(admin).isPresent(),
                "admin row must survive a delete attempt");
    }

    @Test
    void adminCanDeleteNormalUser() throws Exception {
        String admin = uniqueUsername();
        registerUser(admin);
        makeAdmin(admin);
        String victim = uniqueUsername();
        registerUser(victim);
        User victimUser = userRepository.findByUsername(victim).orElseThrow();

        MvcResult loginResult = adminLogin(admin, "password123");
        MockHttpSession session = sessionOf(loginResult);
        MvcResult dash = mockMvc.perform(get("/admin").session(session)).andReturn();

        mockMvc.perform(post("/admin/users/" + victimUser.id() + "/delete")
                        .session(session).param("_csrf", csrf(dash)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?deleted"));

        assertTrue(userRepository.findByUsername(victim).isEmpty());
    }

    @Test
    void adminEditWithUnknownIdShowsErrorPage() throws Exception {
        String admin = uniqueUsername();
        registerUser(admin);
        makeAdmin(admin);

        MvcResult loginResult = adminLogin(admin, "password123");

        mockMvc.perform(get("/admin/users/00000000-0000-0000-0000-000000000000")
                        .session(sessionOf(loginResult))
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
    }
}
