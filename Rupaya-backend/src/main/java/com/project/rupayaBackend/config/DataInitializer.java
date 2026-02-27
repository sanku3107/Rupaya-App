package com.project.rupayaBackend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.project.rupayaBackend.entity.User;
import com.project.rupayaBackend.entity.enums.Role;
import com.project.rupayaBackend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User defaultUser = User.builder()
                .name("Demo Swaroop")
                .email("demo@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(defaultUser);
        log.info("Created default user: demo@example.com / password123");
    }
}
