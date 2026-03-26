package com.pageon.backend.dto.payload;

import com.pageon.backend.dto.common.IdentityVerificationCustomer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationPayload {
    private String otp;
    private IdentityVerificationCustomer customer;
}
