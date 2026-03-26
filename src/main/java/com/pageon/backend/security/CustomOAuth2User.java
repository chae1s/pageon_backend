package com.pageon.backend.security;

import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.common.enums.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oAuth2User;
    private final OAuthUserInfoResponse oAuth2Response;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_USER");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2User.getAttributes();
    }

    @Override
    public String getName() {
        return oAuth2User.getName();
    }

    public OAuthProvider getProvider() {
        return oAuth2Response.getOAuthProvider();
    }

    public String getProviderId() {
        return oAuth2Response.getProviderId();
    }
}
