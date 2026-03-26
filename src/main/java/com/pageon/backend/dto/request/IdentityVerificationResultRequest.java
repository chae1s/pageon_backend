package com.pageon.backend.dto.request;

import com.pageon.backend.common.enums.IdentityProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerificationResultRequest {

    private String di;
    private String otp;
    private String identityProvider;
}
