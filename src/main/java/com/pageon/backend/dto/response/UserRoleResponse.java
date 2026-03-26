package com.pageon.backend.dto.response;

import com.pageon.backend.entity.UserRole;
import lombok.Data;

@Data
public class UserRoleResponse {
    private String roleType;

    public static UserRoleResponse fromEntity(String roleType) {
        UserRoleResponse userRoleResponse = new UserRoleResponse();
        userRoleResponse.setRoleType(roleType);

        return userRoleResponse;
    }
}
