package com.example.taskflow.config;

import com.example.taskflow.domain.Role;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@taskflow.local";

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        String password = generatePassword();
        userService.create(ADMIN_EMAIL, "Admin", password, Role.ADMIN);

        // WARN level so the password is visible regardless of configured log level.
        log.warn("═══════════════════════════════════════════════════════");
        log.warn("  DEFAULT ADMIN ACCOUNT CREATED");
        log.warn("  Email   : {}", ADMIN_EMAIL);
        log.warn("  Password: {}", password);
        log.warn("  Change this password immediately after first login.");
        log.warn("═══════════════════════════════════════════════════════");
    }

    private String generatePassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
