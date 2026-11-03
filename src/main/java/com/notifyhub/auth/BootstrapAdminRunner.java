package com.notifyhub.auth;

import com.notifyhub.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${notifyhub.bootstrap-admin.email}")
    private String email;

    @Value("${notifyhub.bootstrap-admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User(email.toLowerCase(), passwordEncoder.encode(password),
                    "Bootstrap Admin", Role.ADMIN));
            log.info("Bootstrap admin created: {}", email);
        }
    }
}