package com.pageon.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pageon.backend.dto.token.AccessToken;
import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.CustomOauth2UserService;
import com.pageon.backend.security.PrincipalUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@DisplayName("customOauth2UserService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CustomOauth2UserServiceTest {

    @Mock
    private DefaultOAuth2UserService delegate;
    @InjectMocks
    private CustomOauth2UserService customOauth2UserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SocialUserService socialUserService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private ClientRegistration clientRegistration;
    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        lenient().when(objectMapper.convertValue(any(), any(TypeReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    @ParameterizedTest
    @MethodSource("loadUserSource")
    @DisplayName("소셜 로그인 성공 시 accessToken 저장 및 기존 사용자 반환")
    void loadUser_shouldStoreAccessTokenAndReturnUser(String registrationId, Map<String, Object> attributes, OAuthProvider provider, String expectedEmail) {
        // given
        OAuth2User oAuth2User = mock(OAuth2User.class);

        OAuth2AccessToken oAuth2AccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "social-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        when(oAuth2User.getAttributes()).thenReturn(attributes);

        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn(registrationId);

        when(request.getAccessToken()).thenReturn(oAuth2AccessToken);

        when(delegate.loadUser(any())).thenReturn(oAuth2User);

        User user = User.builder()
                .id(1L)
                .email(expectedEmail)
                .oAuthProvider(provider)
                .providerId("123456")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(userRepository.findWithRolesByProviderAndProviderId(provider, "123456")).thenReturn(Optional.of(user));
        
        //when
        OAuth2User result = customOauth2UserService.loadUser(request);
        
        // then
        String redisKey = String.format("user:oauth:token:%s:%d", user.getOAuthProvider().toString(), user.getId());
        verify(valueOperations).set(eq(redisKey), any(AccessToken.class));

        assertInstanceOf(PrincipalUser.class, result);
        assertEquals(expectedEmail, ((PrincipalUser)result).getUsername());
        
    }

    private static Stream<Arguments> loadUserSource() {
        Map<String, Object> kakaoAttr = Map.of(
                "id", "123456",
                "kakao_account", Map.of("email", "test@kakao.com")
        );

        return Stream.of(
                Arguments.of("kakao", kakaoAttr, OAuthProvider.KAKAO, "test@kakao.com"),
                Arguments.of("naver",
                        Map.of("response", Map.of("id", "123456", "email", "test@naver.com")),
                        OAuthProvider.NAVER, "test@naver.com"),
                Arguments.of("google",
                        Map.of("sub", "123456", "email", "test@google.com"),
                        OAuthProvider.GOOGLE, "test@google.com")
        );
    }

    @Test
    @DisplayName("지원하지 않는 provider면 CustomException 발생")
    void loadUser_whenUnsupportedProvider_shouldThrowCustomException() {
        // given
        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        when(delegate.loadUser(any())).thenReturn(mock(OAuth2User.class));
        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("email");

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> customOauth2UserService.loadUser(request)
        );

        // then
        assertEquals("지원하지 않는 OAuth Provider입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_PROVIDER_TYPE, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("신규 유저 소설 로그인 시 signupSocial() 호출")
    void loadUser_whenNewSocialUser_shouldCallSignupSocial() {
        // given
        Map<String, Object> kakaoAttr = Map.of(
                "id", "123456",
                "kakao_account", Map.of("email", "test@kakao.com")
        );
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(delegate.loadUser(any())).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(kakaoAttr);

        OAuth2UserRequest request = mock(OAuth2UserRequest.class);
        when(request.getClientRegistration()).thenReturn(clientRegistration);
        when(clientRegistration.getRegistrationId()).thenReturn("kakao");

        OAuth2AccessToken oAuth2AccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "social-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        when(request.getAccessToken()).thenReturn(oAuth2AccessToken);

        when(userRepository.findWithRolesByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        User newUser = User.builder()
                .id(1L)
                .email("test@kakao.com")
                .oAuthProvider(OAuthProvider.KAKAO)
                .providerId("123456")
                .termsAgreed(true)
                .nickname("randomNickname")
                .build();
        when(socialUserService.signupSocial(any())).thenReturn(newUser);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        //when
        customOauth2UserService.loadUser(request);

        // then
        ArgumentCaptor<OAuthUserInfoResponse> captor = ArgumentCaptor.forClass(OAuthUserInfoResponse.class);
        verify(socialUserService).signupSocial(captor.capture());

        OAuthUserInfoResponse capturedResponse = captor.getValue();
        assertEquals("test@kakao.com", capturedResponse.getEmail());
        assertEquals(OAuthProvider.KAKAO, capturedResponse.getOAuthProvider());
        assertEquals("123456", capturedResponse.getProviderId());

    }



}