package com.pageon.backend.dto.request;

import com.pageon.backend.dto.common.IdentityVerificationCustomer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerificationRequest {
    private IdentityVerificationCustomer customer;
    private String method;
}
