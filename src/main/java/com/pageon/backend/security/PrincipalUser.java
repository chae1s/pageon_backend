package com.pageon.backend.security;

import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.common.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PrincipalUser implements UserDetails, OAuth2User {
    private final User users;
    private final Long id;
    private final String email;
    private final List<RoleType> roleTypes;
    private final OAuthUserInfoResponse oAuthUserInfoResponse;

    // 1. 폼 로그인용 (DB 엔티티 기반)
    public PrincipalUser(User user) {
        this.users = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.roleTypes = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleType())
                .collect(Collectors.toList());
        this.oAuthUserInfoResponse = null;
    }

    // 2. OAuth 로그인용
    public PrincipalUser(User user, OAuthUserInfoResponse oAuthUserInfoResponse) {
        this.users = user;
        this.id = user.getId();
        this.email = user.getEmail();
        this.roleTypes = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleType())
                .collect(Collectors.toList());
        this.oAuthUserInfoResponse = oAuthUserInfoResponse;
    }

    public PrincipalUser(Long id, String email, List<RoleType> roleTypes) {
        this.users = null;
        this.id = id;
        this.email = email;
        this.roleTypes = roleTypes;
        this.oAuthUserInfoResponse = null;
    }


    public User getUsers() {
        return users;
    }
    @Override
    public Map<String, Object> getAttributes() {
        if (oAuthUserInfoResponse == null) {
            return Map.of();
        }
        return Map.of(
                "email", oAuthUserInfoResponse.getEmail(),
                "provider", oAuthUserInfoResponse.getOAuthProvider(),
                "providerID", oAuthUserInfoResponse.getProviderId()
        );
    }

    @Override
    public String getName() {
        return (users != null) ? users.getEmail() : this.email;
    }

    public OAuthProvider getProvider() {
        return oAuthUserInfoResponse.getOAuthProvider();
    }

    public String getProviderId() {
        return oAuthUserInfoResponse.getProviderId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roleTypes.stream()
                .map(roleType -> new SimpleGrantedAuthority(roleType.name()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return (users != null) ? users.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return (users != null) ? users.getEmail() : this.email;
    }

    public Long getId() {
        return (users != null) ? users.getId() : this.id;
    }

    public List<RoleType> getRoleType() {
        if (users != null) {
            return users.getUserRoles().stream()
                    .map(ur -> ur.getRole().getRoleType())
                    .collect(Collectors.toList());
        }
        return this.roleTypes;
    }

}
