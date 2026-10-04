package com.mjdev.SimpleUserManagement.controller;

import com.mjdev.SimpleUserManagement.model.RegisterForm;
import com.mjdev.SimpleUserManagement.model.User;
import com.mjdev.SimpleUserManagement.repository.UserRepository;
import com.mjdev.SimpleUserManagement.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.format.DateTimeFormatter;

@Controller
public class WebController {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserService userService;
    private final UserRepository userRepository;

    public WebController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("loggedIn", isAuthenticated(authentication));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@ModelAttribute RegisterForm form, Model model) {
        try {
            userService.register(form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("form", form);
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/me")
    public String me(Authentication authentication, Model model) {
        model.addAttribute("authName", authentication.getName());
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        model.addAttribute("user", user);
        if (user != null) {
            model.addAttribute("createdAt", format(user.createdAt()));
            model.addAttribute("lastLogin", format(user.lastLogin()));
            model.addAttribute("lastLogoff", format(user.lastLogoff()));
        }
        return "me";
    }

    private String format(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATETIME.format(dateTime);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
