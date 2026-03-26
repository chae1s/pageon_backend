package com.pageon.backend.security;

import com.pageon.backend.dto.response.UserRoleResponse;
import com.pageon.backend.dto.token.TokenInfo;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();

        String redisKey = String.format("user:oauth:code:%d", principalUser.getId());
        String tempCode = UUID.randomUUID().toString().substring(0, 12);

        redisTemplate.opsForValue().set(redisKey, tempCode, 10L, TimeUnit.SECONDS);

        String redirectUrl = UriComponentsBuilder
                .fromUriString("http://localhost:3000/oauth/callback")
                .queryParam("userId", principalUser.getId())
                .queryParam("tempCode", tempCode)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);

    }


}
