package com.pageon.backend.service;

import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.dto.oauth.KakaoSignupRequest;
import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("SocialUserService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class SocialUserServiceTest {
    @InjectMocks
    private SocialUserService SocialUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleService roleService;

    @ParameterizedTest
    @MethodSource("socialSignupSource")
    @DisplayName("소셜 로그인 회원이 존재하지 않으면 신규 가입")
    void signupSocial_shouldCreateNewUser(OAuthUserInfoResponse response, String expectedEmail, OAuthProvider expectedProvider) {

        //when
        User newUser = SocialUserService.signupSocial(response);

        // then
        assertEquals(expectedEmail, newUser.getEmail());
        assertEquals(expectedProvider, newUser.getOAuthProvider());
        verify(roleService).assignDefaultRole(newUser);
        verify(userRepository).save(newUser);
    }

    private static Stream<Arguments> socialSignupSource() {
        Map<String, Object> kakaoAttr = Map.of(
                "id", "providerId",
                "kakao_account", Map.of("email", "test@kakao.com")
        );

        return Stream.of(
                Arguments.of(new KakaoSignupRequest(kakaoAttr), "test@kakao.com", OAuthProvider.KAKAO),
                Arguments.of(Map.of("id", "providerId", "email", "test@naver.com"), OAuthProvider.NAVER),
                Arguments.of(Map.of("id", "providerId", "email", "test@google.com"), OAuthProvider.GOOGLE)
        );

    }


}