package com.orque.crm.auth.repository;

import com.orque.crm.auth.entity.Role;
import com.orque.crm.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
}