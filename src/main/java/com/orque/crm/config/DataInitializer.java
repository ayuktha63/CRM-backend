package com.orque.crm.config;

import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        seedRole(RoleType.ADMIN);
        seedRole(RoleType.SALES_USER);
        seedRole(RoleType.SALES_ADMIN);
        seedRole(RoleType.SALES);
    }

    private void seedRole(RoleType type) {
        if (roleRepository.findByName(type).isEmpty()) {
            roleRepository.save(Role.builder().name(type).build());
        }
    }
}
