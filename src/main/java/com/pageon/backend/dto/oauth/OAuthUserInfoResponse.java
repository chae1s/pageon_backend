package com.pageon.backend.dto.oauth;

import com.pageon.backend.common.enums.OAuthProvider;

public interface OAuthUserInfoResponse {
    OAuthProvider getOAuthProvider();

    String getProviderId();

    String getEmail();

}
