package com.mjdev.SimpleUserManagement.security;

import com.mjdev.SimpleUserManagement.model.User;
import com.mjdev.SimpleUserManagement.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));

        var builder = org.springframework.security.core.userdetails.User
                .withUsername(user.username())
                .password(user.password() == null ? "" : user.password())
                .disabled(!user.enabled());

        if (user.admin()) {
            builder.roles("ADMIN");
        } else {
            builder.roles(user.role());
        }
        return builder.build();
    }
}
