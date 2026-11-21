package com.example.banking.auth.application;

import com.example.banking.auth.domain.Role;
import com.example.banking.auth.domain.User;
import com.example.banking.auth.infrastructure.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdminInitializer {

    @Bean
    ApplicationRunner bootstrapAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password
    ) {
        return args -> {
            if (email.isBlank() || password.isBlank() || userRepository.existsByEmail(email)) {
                return;
            }

            userRepository.save(new User(
                    email.trim().toLowerCase(),
                    passwordEncoder.encode(password),
                    Role.ADMIN
            ));
        };
    }
}
