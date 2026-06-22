package com.orque.crm.config;

import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Override
    public void run(String... args) {

        if (roleRepository.findByName(RoleType.ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                    .name(RoleType.ADMIN)
                    .build();

            roleRepository.save(adminRole);

        }

        if (roleRepository.findByName(RoleType.SALES_USER).isEmpty()) {
            Role salesUserRole = Role.builder()
                    .name(RoleType.SALES_USER)
                    .build();

            roleRepository.save(salesUserRole);
        }
        if (!userRepository.existsByUsername("admin")) {

            Role adminRole = roleRepository
                    .findByName(RoleType.ADMIN)
                    .orElseThrow(() ->
                            new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .username("admin")
                    .email("admin@orque.com")
                    .password(
                            passwordEncoder.encode("Admin@123")
                    )
                    .enabled(true)
                    .role(adminRole)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
        }
    }
}
