package com.pageon.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerificationCustomer {
    private String name;
    private String phoneNumber;
    private String identityNumber;
}
