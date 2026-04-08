package com.pageon.backend.service;

import com.pageon.backend.dto.response.JwtTokenResponse;
import com.pageon.backend.dto.response.ReissuedTokenResponse;
import com.pageon.backend.dto.request.TempCodeRequest;
import com.pageon.backend.dto.token.TokenInfo;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;


    public ReissuedTokenResponse reissueToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        Claims claims = jwtProvider.validateRefreshTokenAndClaims(refreshToken);
        log.info("reissued token is {}", claims);
        Long userId = claims.get("userId", Long.class);

        String redisKey = "user:auth-info:" + userId;
        Long remainTtl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        TokenInfo tokenInfo = (TokenInfo) redisTemplate.opsForValue().getAndDelete(redisKey);

        if (remainTtl <= 0) {
            log.info("reissued token is {}", tokenInfo);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        if (tokenInfo == null) {
            log.error("tokenInfo is null");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findWithRolesById(tokenInfo.getUserId()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        String reissueAccessToken = generateReissueToken(response, user, tokenInfo, remainTtl);

        log.info("access token 만료, refresh token으로 새로운 access token 발급");
        return new ReissuedTokenResponse(reissueAccessToken, true);

    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refreshToken")) {
                    return cookie.getValue();

                }
            }
        }

        throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    private String generateReissueToken(HttpServletResponse response, User user, TokenInfo tokenInfo, Long remainTtl) {
        List<RoleType> roleTypes = getRoleTypes(user);
        String accessToken = jwtProvider.generateAccessToken(tokenInfo.getUserId(), tokenInfo.getEmail(), roleTypes);
        String newRefreshToken = jwtProvider.generateRefreshToken(tokenInfo.getEmail(), user.getId());

        String redisKey = "user:auth-info:" + user.getId();

        saveRefreshTokenInRedis(redisKey, tokenInfo, remainTtl);
        jwtProvider.sendTokens(response, accessToken, newRefreshToken);

        return accessToken;
    }

    private String generateNewToken(HttpServletResponse response, User user) {
        List<RoleType> roleTypes = getRoleTypes(user);
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), roleTypes);
        String refreshToken = jwtProvider.generateRefreshToken(user.getEmail(), user.getId());
        TokenInfo tokenInfo = new TokenInfo(user.getId(), user.getEmail(), refreshToken);

        Long expirationDate = Duration.ofDays(180).getSeconds();
        String redisKey = "user:auth-info:" + user.getId();

        saveRefreshTokenInRedis(redisKey, tokenInfo, expirationDate);
        jwtProvider.sendTokens(response, accessToken, refreshToken);

        return accessToken;
    }

    private List<RoleType> getRoleTypes(User user) {
        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleType())
                .toList();
    }

    private void saveRefreshTokenInRedis(String redisKey, TokenInfo tokenInfo, Long remainTtl) {
        try {
            redisTemplate.opsForValue().set(redisKey, tokenInfo, Duration.ofSeconds(remainTtl));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    public JwtTokenResponse exchangeCode(HttpServletResponse response, TempCodeRequest tempCodeRequest) {
        String redisKey = String.format("user:oauth:code:%d", tempCodeRequest.getUserId());

        String tempCode = (String) redisTemplate.opsForValue().getAndDelete(redisKey);
        
        if (!tempCodeRequest.getTempCode().equals(tempCode)) {
            throw new CustomException(ErrorCode.INVALID_TEMP_CODE);
        }

        User user = userRepository.findWithRolesById(tempCodeRequest.getUserId()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        String accessToken = generateNewToken(response, user);

        List<String> userRoles = getRoleTypes(user).stream()
                .map(RoleType::toString)
                .toList();

        String targetPath = null;
        if (userRoles.contains("ROLE_CREATOR")) {
            targetPath = "/creators/dashboard";
        } else if (userRoles.contains("ROLE_ADMIN")) {
            targetPath = "/admin/dashboard";
        }

        return new JwtTokenResponse(true, accessToken, user.getOAuthProvider(), userRoles, targetPath);

    }


}
