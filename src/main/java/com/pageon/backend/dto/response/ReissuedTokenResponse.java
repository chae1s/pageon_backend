package com.pageon.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReissuedTokenResponse {
    private String accessToken;
    private Boolean isRefreshed;
}
