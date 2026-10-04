package com.mjdev.SimpleUserManagement.controller;

import com.mjdev.SimpleUserManagement.model.AdminEditForm;
import com.mjdev.SimpleUserManagement.model.AdminUserRow;
import com.mjdev.SimpleUserManagement.model.User;
import com.mjdev.SimpleUserManagement.repository.UserRepository;
import com.mjdev.SimpleUserManagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping
    public String dashboard(Model model) {
        List<AdminUserRow> users = userRepository.findAll().stream()
                .map(this::toRow)
                .toList();
        model.addAttribute("users", users);
        return "admin/dashboard";
    }

    @GetMapping("/users/{id}")
    public String edit(@PathVariable String id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("user", user);
        model.addAttribute("createdAt", format(user.createdAt()));
        model.addAttribute("lastLogin", format(user.lastLogin()));
        model.addAttribute("lastLogoff", format(user.lastLogoff()));
        model.addAttribute("form", new AdminEditForm(user.username(), user.nickname(), user.email(), ""));
        return "admin/edit";
    }

    @PostMapping("/users/{id}")
    public String save(@PathVariable String id, @ModelAttribute AdminEditForm form, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            userService.adminUpdateUser(id, form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("createdAt", format(user.createdAt()));
            model.addAttribute("lastLogin", format(user.lastLogin()));
            model.addAttribute("lastLogoff", format(user.lastLogoff()));
            model.addAttribute("form", form);
            return "admin/edit";
        }
        return "redirect:/admin?saved";
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.admin()) {
            // admins cannot delete other admins (or themselves)
            return "redirect:/admin?delete-denied";
        }
        userRepository.deleteById(id);
        return "redirect:/admin?deleted";
    }

    private AdminUserRow toRow(User u) {
        return new AdminUserRow(
                u.id(), u.username(), u.password(), u.nickname(), u.email(), u.avatarUrl(),
                u.role(), u.enabled(), u.admin(), u.oauth2Provider(), u.oauth2Handle(),
                format(u.createdAt()), format(u.lastLogin()), format(u.lastLogoff()));
    }

    private String format(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATETIME.format(dateTime);
    }
}
