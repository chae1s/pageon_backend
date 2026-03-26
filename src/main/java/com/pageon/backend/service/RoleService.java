package com.pageon.backend.service;

import com.pageon.backend.entity.Role;
import com.pageon.backend.entity.UserRole;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public void assignDefaultRole(User user) {
        Role role = roleRepository.findByRoleType(RoleType.ROLE_USER).orElseThrow(
                () -> new CustomException(ErrorCode.ROLE_NOT_FOUND)
        );

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();

        user.getUserRoles().add(userRole);
    }
}
