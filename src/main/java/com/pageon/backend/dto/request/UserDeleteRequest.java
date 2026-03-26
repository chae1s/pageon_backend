package com.pageon.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDeleteRequest {
    private String password;
    private Integer reasonIndex;
    private String reason;
    private String otherReason;
}
