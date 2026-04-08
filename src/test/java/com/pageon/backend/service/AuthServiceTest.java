package com.pageon.backend.service;

import com.pageon.backend.dto.request.TempCodeRequest;
import com.pageon.backend.dto.response.JwtTokenResponse;
import com.pageon.backend.dto.response.ReissuedTokenResponse;
import com.pageon.backend.dto.token.TokenInfo;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("AuthService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @InjectMocks
    private AuthService authService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Claims claims;
    
    @Test
    @DisplayName("accessToken 만료 후 유효한 refreshToken으로 새로운 accessToken 발급")
    void reissueToken_withValidRefreshToken_shouldReturnJwtTokenResponse() {
        // given
        String refreshToken = "sample-refresh-token";
        TokenInfo tokenInfo = new TokenInfo(1L, "test@mail.com", refreshToken);

        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .oAuthProvider(OAuthProvider.EMAIL)
                .build();

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", refreshToken)
        });
        String redisKey = "user:auth-info:" + userId;
        when(jwtProvider.validateRefreshTokenAndClaims(refreshToken)).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getExpire(eq(redisKey), any())).thenReturn(3600L);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(tokenInfo);

        when(userRepository.findWithRolesById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.generateAccessToken(any(), any(), any())).thenReturn("new-access-token");
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("new-refresh-token");

        //when
        ReissuedTokenResponse result = authService.reissueToken(request, response);
        
        // then
        assertEquals("new-access-token", result.getAccessToken());
        assertTrue(result.getIsRefreshed());
        verify(jwtProvider).sendTokens(eq(response), eq("new-access-token"), eq("new-refresh-token"));
        
    }
    
    @Test
    @DisplayName("cookie 안에 refreshToken이 존재하지 않을 때 CustomException 발생")
    void reissueToken_withNoExistingRefreshToken_shouldThrowCustomException() {
        // given
        when(request.getCookies()).thenReturn(new Cookie[]{});

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(request, response)
        );
        
        // then
        assertEquals("Refresh Token이 존재하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }
    
    @Test
    @DisplayName("redis에 refreshToken로 저장된 정보가 존재하지 않을 때 CustomException 발생")
    void reissueToken_withNoExistingTokenInfo_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String refreshToken = "sample-refresh-token";

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", refreshToken)
        });
        String redisKey = "user:auth-info:" + userId;

        when(jwtProvider.validateRefreshTokenAndClaims(refreshToken)).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getExpire(eq(redisKey), any())).thenReturn(3600L);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(null);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(request, response)
        );
        
        // then
        assertEquals("유효하지 않은 토큰입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_TOKEN, ErrorCode.valueOf(exception.getErrorCode()));
        
    }
    
    @Test
    @DisplayName("cookie에서 가져온 refresh token의 userId 정보로 가입된 User를 찾을 수 없을 때 UsernameNotFoundException 발생")
    void reissueToken_withInvalidUserId_shouldThrowUsernameNotFoundException() {
        // given
        String refreshToken = "sample-refresh-token";


        Long userId = 1L;
        TokenInfo tokenInfo = new TokenInfo(userId, "test@mail.com", refreshToken);

        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", refreshToken)
        });

        String redisKey = "user:auth-info:" + userId;

        when(jwtProvider.validateRefreshTokenAndClaims(refreshToken)).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getExpire(eq(redisKey), any())).thenReturn(3600L);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(tokenInfo);

        when(userRepository.findWithRolesById(userId)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(request, response)
        );
        
        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }
    

    @Test
    @DisplayName("refresh token의 유효기간이 0 이하면 CustomException 발생")
    void reissueToken_withExpiredTtl_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String refreshToken = "sample-refresh-token";
        TokenInfo tokenInfo = new TokenInfo(1L, "test@mail.com", refreshToken);


        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("refreshToken", refreshToken)
        });

        String redisKey = "user:auth-info:" + 1L;

        when(jwtProvider.validateRefreshTokenAndClaims(refreshToken)).thenReturn(claims);
        when(claims.get("userId", Long.class)).thenReturn(userId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.getExpire(eq(redisKey), any())).thenReturn(-2L);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(tokenInfo);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.reissueToken(request, response)
        );

        // then
        assertEquals("유효하지 않은 토큰입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_TOKEN, ErrorCode.valueOf(exception.getErrorCode()));

    }

    @Test
    @DisplayName("임시 코드 일치 시 access token과 refresh token 발급")
    void exchangeCode_whenTempCodeMatch_shouldReturnJwtTokenResponse() {
        // given
        Long userId = 1L;
        String tempCode = "temp-code";
        TempCodeRequest tempCodeRequest = new TempCodeRequest(userId, tempCode);

        String redisKey = "user:oauth:code:" + userId;

        Long expirationDate = Duration.ofDays(180).getSeconds();

        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .userRoles(new ArrayList<>())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(tempCode);
        when(userRepository.findWithRolesById(userId)).thenReturn(Optional.of(user));

        when(jwtProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        //when
        JwtTokenResponse result = authService.exchangeCode(response, tempCodeRequest);

        // then
        assertTrue(result.getIsLogin());
        assertEquals("access-token", result.getAccessToken());

        String refreshTokenRedisKey = "user:auth-info:" + userId;
        verify(valueOperations).set(
                eq(refreshTokenRedisKey),
                any(TokenInfo.class),
                eq(Duration.ofSeconds(expirationDate))
        );

        verify(jwtProvider).sendTokens(eq(response), eq("access-token"), eq("refresh-token"));

    }

    @Test
    @DisplayName("임시 코드가 일치하지 않으면 CustomException 발생")
    void exchangeCode_withNotMatchTempCode_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String tempCode = "redis-temp-code";
        TempCodeRequest tempCodeRequest = new TempCodeRequest(userId, "request-temp-code");

        String redisKey = "user:oauth:code:1";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(tempCode);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.exchangeCode(response, tempCodeRequest)
        );

        // then
        assertEquals(ErrorCode.INVALID_TEMP_CODE, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("임시 코드가 일치하지 않습니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("사용자 정보를 찾을 수 없으면 CustomException 발생")
    void exchangeCode_withNonExistUser_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String tempCode = "temp-code";
        TempCodeRequest tempCodeRequest = new TempCodeRequest(userId, tempCode);

        String redisKey = "user:oauth:code:1";

        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .userRoles(new ArrayList<>())
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(redisKey)).thenReturn(tempCode);
        when(userRepository.findWithRolesById(userId)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> authService.exchangeCode(response, tempCodeRequest)
        );

        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));


    }



}