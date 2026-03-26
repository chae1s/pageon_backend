package com.pageon.backend.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pageon.backend.dto.token.AccessToken;
import com.pageon.backend.dto.oauth.GoogleSignupRequest;
import com.pageon.backend.dto.oauth.KakaoSignupRequest;
import com.pageon.backend.dto.oauth.NaverSignupRequest;
import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.service.SocialUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOauth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate;
    private final UserRepository userRepository;
    private final SocialUserService socialUserService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuthUserInfoResponse userInfoResponse = switch (registrationId) {
            case "kakao" -> new KakaoSignupRequest(oAuth2User.getAttributes());
            case "naver" -> {
                Object response = oAuth2User.getAttributes().get("response");
                Map<String, Object> attribute = objectMapper.convertValue(response, new TypeReference<>() {});

                yield new NaverSignupRequest(attribute);
            }
            case "google" -> new GoogleSignupRequest(oAuth2User.getAttributes());
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER_TYPE);
        };

        User user = existingUser(userInfoResponse);

        setAccessToken(userRequest.getAccessToken(), user);
        return new PrincipalUser(user, userInfoResponse);
    }


    private User existingUser(OAuthUserInfoResponse response) {

        return userRepository.findWithRolesByProviderAndProviderId(
                response.getOAuthProvider(), response.getProviderId()
        ).orElseGet(() -> socialUserService.signupSocial(response));

    }


    private void setAccessToken(OAuth2AccessToken oAuth2AccessToken, User user) {
        String accessToken = oAuth2AccessToken.getTokenValue();

        String redisKey = String.format("user:oauth:token:%s:%d", user.getOAuthProvider().toString(), user.getId());

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        AccessToken socialAccessToken = new AccessToken().updateAccessToken(user.getId(), accessToken);
        valueOperations.set(redisKey, socialAccessToken);
    }

}
