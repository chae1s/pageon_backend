package com.pageon.backend.repository;

import com.pageon.backend.entity.Role;
import com.pageon.backend.common.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleType(RoleType roleType);
}
