package com.mjdev.SimpleUserManagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.mjdev.SimpleUserManagement.repository.UserRepository;
import com.mjdev.SimpleUserManagement.security.GoogleOidcUserService;
import com.mjdev.SimpleUserManagement.security.LastLoginSuccessHandler;
import com.mjdev.SimpleUserManagement.security.LastLogoffLogoutHandler;
import com.mjdev.SimpleUserManagement.security.OAuthRegistrationFailureHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final OAuthRegistrationFailureHandler oAuthRegistrationFailureHandler;
    private final GoogleOidcUserService googleOidcUserService;
    private final LastLogoffLogoutHandler lastLogoffLogoutHandler;

    public SecurityConfig(UserRepository userRepository,
                          OAuthRegistrationFailureHandler oAuthRegistrationFailureHandler,
                          GoogleOidcUserService googleOidcUserService,
                          LastLogoffLogoutHandler lastLogoffLogoutHandler) {
        this.userRepository = userRepository;
        this.oAuthRegistrationFailureHandler = oAuthRegistrationFailureHandler;
        this.googleOidcUserService = googleOidcUserService;
        this.lastLogoffLogoutHandler = lastLogoffLogoutHandler;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    LastLoginSuccessHandler userLoginSuccessHandler() {
        return new LastLoginSuccessHandler(userRepository, "/me");
    }

    @Bean
    LastLoginSuccessHandler adminLoginSuccessHandler() {
        return new LastLoginSuccessHandler(userRepository, "/admin");
    }

    /**
     * Admin area: own login screen, requires ROLE_ADMIN.
     */
    @Bean
    @Order(1)
    SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/admin/login")
                .successHandler(adminLoginSuccessHandler())
                .permitAll()) //optional
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .addLogoutHandler(lastLogoffLogoutHandler)
                .logoutSuccessUrl("/admin/login?logout")
                .permitAll());
        return http.build();
    }

    /**
     * Everything else: user login (form or Google).
     */
    @Bean
    @Order(2)
    SecurityFilterChain mainSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(userLoginSuccessHandler())
                .permitAll()) //optional
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                .successHandler(userLoginSuccessHandler())
                .failureHandler(oAuthRegistrationFailureHandler))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler(lastLogoffLogoutHandler)
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }
}
