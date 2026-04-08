package com.pageon.backend.service;

import com.pageon.backend.dto.oauth.KakaoSignupRequest;
import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.dto.request.*;
import com.pageon.backend.dto.response.JwtTokenResponse;
import com.pageon.backend.dto.response.UserInfoResponse;
import com.pageon.backend.dto.token.AccessToken;
import com.pageon.backend.dto.token.TokenInfo;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.JwtProvider;
import com.pageon.backend.security.PrincipalUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.*;


import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@DisplayName("userService 단위 테스트")
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private MailService mailService;
    @Mock
    private RestTemplate restTemplate;
    private PrincipalUser mockPrincipalUser;
    @Mock
    private ValueOperations<String, Object> valueOperations;


    @BeforeEach
    void setUp() {
        mockPrincipalUser = mock(PrincipalUser.class);
    }

    private SignupRequest validRequest() {
        return SignupRequest.builder()
                .email("test@mail.com")
                .password("!test1234")
                .nickname("테스터")
                .birthDate("19990101")
                .gender("FEMALE")
                .termsAgreed(true)
                .build();
    }

    @Test
    @DisplayName("모든 정보가 유효할 때 회원가입 성공")
    void signup_withValidInfo_shouldSucceed() {
        // given
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(roleService).assignDefaultRole(any(User.class));
        when(passwordEncoder.encode(any())).thenReturn("encodePassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        //when
        userService.signup(validRequest());
        
        // then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("test@mail.com", savedUser.getEmail());
        assertEquals("테스터", savedUser.getNickname());
        assertEquals("encodePassword", savedUser.getPassword());
    }

    @Test
    @DisplayName("회원가입 시 권한 부여 메소드가 호출된다.")
    void signup_shouldCreateUserRoleWithDefaultRole() {
        // given

        when(passwordEncoder.encode(any())).thenReturn("encodePassword");


        //when
        userService.signup(validRequest());
        
        // then
        verify(roleService, times(1)).assignDefaultRole(any(User.class));
    }

    @Test
    @DisplayName("회원가입 시 provider는 EMAIL, providerId는 null로 저장")
    void signup_shouldSetProviderAsEmailAndProviderIdAsNull() {
        // given

        when(passwordEncoder.encode(any())).thenReturn("encodePassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        //when
        userService.signup(validRequest());
        
        // then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(OAuthProvider.EMAIL, savedUser.getOAuthProvider(), "이메일 가입 시 OAuthProvider는 EMAIL이어야 함.");
        
        assertNull(savedUser.getProviderId(), "이메일 가입 시 ProviderId가 null이어야 함.");
    }
    
    @Test
    @DisplayName("이메일이 중복이 아닐 때 false 리턴")
    void isEmailDuplicate_withNonExistingEmail_shouldReturnFalse() {
        // given
        String email = "test1@mail.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        //when
        boolean result = userService.isEmailDuplicate(email);
        
        // then
        assertFalse(result, "중복이 아닌 이메일일 경우 false를 반환");
        
    }

    @Test
    @DisplayName("이메일이 중복일 때 true 리턴")
    void signup_withExistingEmail_shouldReturnTrue() {
        // given
        String email = "test@mail.com";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        //when
        boolean result = userService.isEmailDuplicate(email);

        // then
        assertTrue(result, "중복인 이메일일 경우 true를 반환");

    }

    @Test
    @DisplayName("닉네임이 중복이 아닐 때 false 리턴")
    void signup_withNonExistingNickname_shouldReturnFalse() {
        // given
        String nickname = "nickname";

        when(userRepository.existsByNickname(nickname)).thenReturn(false);
        //when
        boolean result = userService.isNicknameDuplicate(nickname);

        // then
        assertFalse(result, "중복이 아닌 닉네임일 경우 false를 반환");

    }

    @Test
    @DisplayName("닉네임이 중복일 경우 true 리턴")
    void signup_withExistingNickname_shouldReturnTrue() {
        // given
        String nickname = "nickname";

        when(userRepository.existsByNickname(nickname)).thenReturn(true);

        //when
        boolean result = userService.isNicknameDuplicate(nickname);

        // then
        assertTrue(result, "중복인 닉네임일 경우 true를 반환");

    }

    @Test
    @DisplayName("유효한 이메일, 비밀번호로 로그인 시 토큰 발급 및 loginCheck true return")
    void login_withValidEmailAndPassword_shouldReturnAccessAndRefreshToken() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();
        String redisKey = "user:auth-info:" + userId;

        LoginRequest loginRequest = new LoginRequest("test@mail.com", "!test1234");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipalUser);
        when(mockPrincipalUser.getId()).thenReturn(userId);
        when(mockPrincipalUser.getUsername()).thenReturn("test@mail.com");
        when(mockPrincipalUser.getRoleType()).thenReturn(List.of(RoleType.ROLE_USER));
        when(mockPrincipalUser.getUsers()).thenReturn(user);

        when(jwtProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        //when
        JwtTokenResponse result = userService.login(loginRequest, response);

        // then
        assertTrue(result.getIsLogin());
        assertEquals("access-token", result.getAccessToken());
        assertEquals(OAuthProvider.EMAIL, result.getOAuthProvider());

        verify(valueOperations).set(
                eq(redisKey),
                any(TokenInfo.class),
                eq(Duration.ofDays(180))
        );

        verify(jwtProvider).sendTokens(eq(response), eq("access-token"), eq("refresh-token"));

    }

    @Test
    @DisplayName("토큰 생성 실패 시 CustomException 발생")
    void login_whenTokenGenerationFails_shouldThrowCustomException() {
        // given
        LoginRequest loginRequest = new LoginRequest("test@mail.com", "!test1234");

        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipalUser);
        when(mockPrincipalUser.getId()).thenReturn(1L);
        when(mockPrincipalUser.getUsername()).thenReturn("test@mail.com");
        when(mockPrincipalUser.getRoleType()).thenReturn(List.of(RoleType.ROLE_USER));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipalUser);

        when(jwtProvider.generateAccessToken(any(), any(), any())).thenReturn(null);
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");


        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.login(loginRequest, response);
        });

        // then
        assertEquals("Refresh Token 또는 Access Token 생성에 실패했습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.TOKEN_GENERATION_FAILED, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("이메일이 존재하지 않거나 비밀번호가 틀려 인증 실패")
    void login_withInvalidEmailOrPassword_shouldThrowAuthenticationException() {
        // given
        LoginRequest loginRequest = new LoginRequest("test@mail.com", "!test1234");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("잘못된 이메일 또는 비밀번호입니다."));

        //when & then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            userService.login(loginRequest, response);
        });

        assertEquals("잘못된 이메일 또는 비밀번호입니다.", exception.getMessage());
        
    }

    @Test
    @DisplayName("토큰 발급에 성공했지만 redis에 저장 중 CustomException 발생")
    void login_whenRedisStorageFails_shouldThrowCustomException() {
        // given
        LoginRequest loginRequest = new LoginRequest("test@mail.com", "!test1234");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockPrincipalUser);
        when(mockPrincipalUser.getUsername()).thenReturn("test@mail.com");
        when(mockPrincipalUser.getId()).thenReturn(1L);

        when(jwtProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new CustomException(ErrorCode.REDIS_CONNECTION_FAILED)).when(valueOperations).set(any(), any(), any());

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.login(loginRequest, response);
        });

        // then
        assertEquals("Redis 연결에 실패했습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.REDIS_CONNECTION_FAILED, ErrorCode.valueOf(exception.getErrorCode()));

    }
    
    @Test
    @DisplayName("정상적인 로그아웃 후 쿠키 제거 및 토큰 제거")
    void logout_withValidUser_shouldDeletedCookieAndDeleteToken() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .build();

        String redisKey = "user:auth-info:" + userId;

        when(mockPrincipalUser.getId()).thenReturn(userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", "sample-refresh-token")
        });
        TokenInfo tokenInfo = new TokenInfo(1L, "test@mail.com", "sample-refresh-token");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(tokenInfo);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);

        //when
        userService.logout(mockPrincipalUser, request, response);
        
        // then
        verify(userRepository).findByIdAndDeletedAtIsNull(userId);
        verify(valueOperations).get(redisKey);
        verify(redisTemplate).delete(redisKey);

        verify(response).addCookie(cookieCaptor.capture());
        Cookie clearedCookie = cookieCaptor.getValue();
        assertEquals("refreshToken", clearedCookie.getName());
        assertNull(clearedCookie.getValue());
        assertEquals(0, clearedCookie.getMaxAge());
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자일 경우 CustomException 발생")
    void logout_withNonExistUser_shouldThrowCustomException() {
        // given
        Long userId = 1L;

        when(mockPrincipalUser.getId()).thenReturn(userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.logout(mockPrincipalUser, request, response)
        );
        
        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }

    @Test
    @DisplayName("Redis에 tokenInfo가 없으면 토큰 삭제를 하지 않는다.")
    void logout_whenTokenInfoNotInRedis_shouldNotDeleteToken() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .build();

        String redisKey = "user:auth-info:" + userId;

        when(mockPrincipalUser.getId()).thenReturn(userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", "sample-refresh-token")
        });

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        //when & then
        assertDoesNotThrow(() -> userService.logout(mockPrincipalUser, request, response));
        verify(redisTemplate, never()).delete(anyString());
    }
    
    @Test
    @DisplayName("refreshToken이 없으면 CustomException 발생")
    void logout_withoutRefreshToken_shouldThrowCustomException() {
        // given
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .build();

        when(mockPrincipalUser.getId()).thenReturn(userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        Cookie cookie = new Cookie("NoRefreshToken", "sample-refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[]{
                cookie
        });

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.logout(mockPrincipalUser, request, response);
        });

        // then
        assertEquals("Refresh Token이 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }

    @Test
    @DisplayName("token의 userId가 불일치하면 CustomException 발생")
    void logout_whenUserIdMismatch_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        String redisKey = "user:auth-info:" + userId;

        when(mockPrincipalUser.getId()).thenReturn(userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", "sample-refresh-token")
        });
        TokenInfo tokenInfo = new TokenInfo(999L, "test2@mail.com", "other-refresh-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(tokenInfo);


        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.logout(mockPrincipalUser, request, response) );


        // then
        assertEquals(ErrorCode.INVALID_TOKEN, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("유효하지 않은 토큰입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("이메일 회원일 경우 임시 비밀번호 발급 후 메일 전송")
    void passwordFind_withExistingEmailUser_shouldSendTempPasswordByEmail() {
        // given
        String email = "test@mail.com";

        User user = User.builder()
                .id(1L)
                .email(email)
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("encodedTempPassword");

        //when
        Map<String, String> result = userService.passwordFind(new FindPasswordRequest(email));

        // then
        assertEquals("email", result.get("type"));
        assertEquals("임시 비밀번호가 메일로 발송되었습니다.", result.get("message"));

        verify(passwordEncoder).encode(any());
        verify(mailService).sendTemporaryPassword(eq(email), any());

    }

    @Test
    @DisplayName("소셜 로그인 회원일 경우 비밀번호 발급 없이 안내메세지 반환")
    void passwordFind_withSocialProviderUser_shouldReturnSocialMessage() {
        // given
        String email = "test@mail.com";
        OAuthProvider provider = OAuthProvider.NAVER;

        User user = User.builder()
                .email(email)
                .password("encodePassword")
                .oAuthProvider(provider)
                .build();

        when(userRepository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.of(user));
        //when
        Map<String, String> result = userService.passwordFind(new FindPasswordRequest(email));

        // then
        assertEquals("social", result.get("type"));
        assertEquals(String.format("%s로 회원가입된 이메일입니다.", user.getOAuthProvider()), result.get("message"));

    }

    @Test
    @DisplayName("존재하지 않는 이메일일 경우 안내메세지 리턴")
    void passwordFind_withNonExistingEmailUser_shouldReturnNoUserMessage() {
        // given
        String email = "test@mail.com";

        when(userRepository.findByEmailAndDeletedAtIsNull(email)).thenReturn(Optional.empty());

        //when
        Map<String, String> result = userService.passwordFind(new FindPasswordRequest(email));

        // then
        assertEquals("noUser", result.get("type"));
        assertEquals("회원가입되지 않은 이메일입니다.", result.get("message"));

    }
    
    @Test
    @DisplayName("존재하는 사용자의 정보를 조회하면 UserInfoResponse를 리턴")
    void getMyInfo_withValidPrincipal_shouldReturnUserInfoResponse() {
        // given
        Long userId = 1L;
        String email = "test@mail.com";
        User user = User.builder()
                .id(userId)
                .email(email)
                .nickname("nickname")
                .oAuthProvider(OAuthProvider.EMAIL)
                .pointBalance(0)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        //when
        UserInfoResponse userInfoResponse = userService.getMyInfo(userId);
        
        // then
        assertEquals(email, userInfoResponse.getEmail());
        assertEquals("nickname", userInfoResponse.getNickname());
        
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 CustomException 발생")
    void getMyInfo_withInvalidUserId_shouldThrowCustomException() {
        // given
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.getMyInfo(1L)
        );

        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));

    }

    
    @Test
    @DisplayName("입력한 비밀번호와 사용자의 정보 속 비밀번호가 일치하면 true 리턴")
    void checkPassword_withCorrectPassword_shouldReturnTrue() {
        // given
        Long userId = 1L;
        String password = "encodePassword";
        User user = User.builder()
                .id(userId)
                .password(password)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "encodePassword")).thenReturn(true);
        //when
        boolean result = userService.checkPassword(1L, password);
        
        // then
        assertTrue(result);
        
    }
    
    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 CustomException 발생")
    void checkPassword_withInvalidUser_shouldThrowCustomException() {
        // given
        String password = "encodePassword";

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.checkPassword(1L, password);
        });
        
        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }
    
    @Test
    @DisplayName("입력한 비밀번호와 사용자 정보 속 비밀번호가 일치하지 않으면 false 리턴")
    void checkPassword_withWrongPassword_shouldReturnFalse() {
        // given
        Long userId = 1L;
        String password = "encodePassword";
        User user = User.builder()
                .id(userId)
                .password(password)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        //when
        boolean result = userService.checkPassword(1L, password);
        
        // then
        assertFalse(result);
        
    }
    
    @Test
    @DisplayName("닉네임만 수정할 경우 닉네임이 정상 변경됨")
    void updateProfile_withValidNicknameOnly_shouldUpdateNickname() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .nickname("nickname")
                .build();

        String newNickname = "newNick";
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest(null, newNickname);

        //when
        userService.updateProfile(userId, userUpdateRequest);

        // then
        assertEquals(newNickname, user.getNickname());

    }

    @Test
    @DisplayName("닉네임이 빈 문자열이면 닉네임이 변경되지 않는다.")
    void updateProfile_withBlankNickname_shouldNotUpdateNickname() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .nickname("nickname")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest(null, " ");

        //when
        userService.updateProfile(userId, userUpdateRequest);

        // then
        assertEquals("nickname", user.getNickname());

    }

    @Test
    @DisplayName("비밀번호만 수정할 경우 비밀번호가 정상 변경됨")
    void updateProfile_withValidPasswordOnly_shouldUpdatePassword() {
        // given
        String encodeNewPassword = "encodeNewPassword";
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn(encodeNewPassword);

        UserUpdateRequest userUpdateRequest = new UserUpdateRequest("!test1234", null);
        //when
        userService.updateProfile(userId, userUpdateRequest);

        // then
        assertEquals(encodeNewPassword, user.getPassword());

    }

    @Test
    @DisplayName("비밀번호가 빈 문자열이면 비밀번호가 변경되지 않는다.")
    void updateProfile_withBlankPassword_shouldNotUpdatePassword() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        UserUpdateRequest userUpdateRequest = new UserUpdateRequest(" ", null);

        //when
        userService.updateProfile(userId, userUpdateRequest);

        // then
        assertEquals("password", user.getPassword());
        verify(passwordEncoder, never()).encode(anyString());

    }

    @Test
    @DisplayName("닉네임과 비밀번호 모두 수정할 경우 둘 다 반영됨")
    void updateProfile_withValidNicknameAndPassword_shouldUpdateBoth() {
        // given
        Long userId = 1L;
        String newEncodePassword = "newEncodePassword";
        String newNickname = "newNickname";
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .nickname("nickname")
                .oAuthProvider(OAuthProvider.EMAIL)
                .providerId(null)
                .pointBalance(0)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn(newEncodePassword);

        UserUpdateRequest userUpdateRequest = new UserUpdateRequest("!test1234", newNickname);

        //when
        userService.updateProfile(userId, userUpdateRequest);

        // then
        assertEquals(newNickname, user.getNickname());
        assertEquals(newEncodePassword, user.getPassword());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 수정 시 CustomException 발생")
    void updateProfile_withInvalidUserId_shouldThrowCustomException() {
        // given
        Long userId = 1L;

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.updateProfile(userId, userUpdateRequest)
        );

        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("비밀번호 형식이 유효하지 않을 경우 예외 발생")
    void updateProfile_withInvalidPassword_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .nickname("nickname")
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        String invalidPassword = "abc1234";
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest(invalidPassword, null);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.updateProfile(userId, userUpdateRequest)
        );

        // then
        assertEquals("비밀번호는 8자 이상, 영문, 숫자, 특수문자(!@-#$%&^)를 모두 포함해야 합니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.PASSWORD_POLICY_VIOLATION, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("이메일 회원 탈퇴 시 비밀번호가 일치하면 계정 삭제")
    void deleteAccount_withCorrectPassword_shouldDeleteAccount() {
        // given
        Long userId = 1L;
        String password = "password";
        String email = "test@mail.com";
        String nickname = "nickname";

        User user = User.builder()
                .id(userId)
                .email(email)
                .password(password)
                .nickname(nickname)
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();

        String redisKey = "user:auth-info:" + userId;
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq(password))).thenReturn(true);

        Cookie cookie = new Cookie("refreshToken", "sample-refresh-token");
        when(request.getCookies()).thenReturn(new Cookie[]{
                cookie
        });

        TokenInfo tokenInfo = new TokenInfo(1L, "test@mail.com", "sample-refresh-token");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(tokenInfo);

        //when
        Map<String, Object> result = userService.deleteAccount(
                userId, new UserDeleteRequest(password, 1, "", ""), request
        );

        // then
        assertTrue((boolean) result.get("isDeleted"));
        assertEquals("계정이 삭제되었습니다.", result.get("message"));

        assertTrue(user.getEmail().startsWith("delete_"));
        assertTrue(user.getNickname().startsWith("delete_"));

        assertNotNull(user.getDeletedAt());

        verify(redisTemplate).delete("sample-refresh-token");
    }

    @Test
    @DisplayName("이메일 회원 탈퇴 시 비밀번호가 일치하지 않으면 계정 삭제 안됨")
    void deleteAccount_withInvalidPassword_shouldNotDeleteAccount() {
        // given
        Long userId = 1L;
        String password = "password";

        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password(password)
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq(password))).thenReturn(false);

        //when
        Map<String, Object> result = userService.deleteAccount(
                userId, new UserDeleteRequest(password, 1, "", ""), request
        );

        // then
        assertFalse((boolean) result.get("isDeleted"));
        assertEquals("비밀번호가 일치하지 않습니다.", result.get("message"));

    }

    @Test
    @DisplayName("존재하지 않는 사용자일 경우 CustomException 발생")
    void deleteAccount_withInvalidUser_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String password = "password";
        request = mock(HttpServletRequest.class);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> userService.deleteAccount(userId, new UserDeleteRequest(), request)
        );

        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @ParameterizedTest
    @DisplayName("소셜 로그인 유저는 연결 해제 후 계정 삭제된다.")
    @MethodSource("socialProviderSource")
    void deleteAccount_withSocialUser_shouldUnlinkAndDelete(OAuthProvider provider, String expectedMessage) {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .nickname("nickname")
                .oAuthProvider(provider)
                .providerId("sampleProviderId")
                .build();
        String refreshTokenRedisKey = "user:auth-info:" + userId;
        String socialAccessToken = "social-access-token";
        String redisKey = String.format("user:oauth:token:%s:%d", user.getOAuthProvider().toString(), user.getId());

        AccessToken accessToken = new AccessToken(userId, socialAccessToken);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(accessToken);

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", "sample-refresh-token")
        });

        TokenInfo tokenInfo = new TokenInfo(1L, "test@mail.com", "sample-refresh-token");
        when(valueOperations.get(refreshTokenRedisKey)).thenReturn(tokenInfo);

        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        //when
        Map<String, Object> result = userService.deleteAccount(userId, null, request);

        // then
        assertTrue((boolean) result.get("isDeleted"));
        assertEquals(expectedMessage, result.get("message"));

        verify(redisTemplate).delete(redisKey);
    }

    static Stream<Arguments> socialProviderSource() {
        return Stream.of(
                Arguments.of(OAuthProvider.KAKAO, "카카오 계정이 삭제되었습니다."),
                Arguments.of(OAuthProvider.NAVER, "네이버 계정이 삭제되었습니다."),
                Arguments.of(OAuthProvider.GOOGLE, "구글 계정이 삭제되었습니다.")
        );
    }

}
