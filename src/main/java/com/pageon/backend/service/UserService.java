package com.pageon.backend.service;

import com.pageon.backend.common.enums.Gender;
import com.pageon.backend.common.enums.RoleType;
import com.pageon.backend.dto.oauth.OAuthUserInfoResponse;
import com.pageon.backend.dto.request.*;
import com.pageon.backend.dto.response.JwtTokenResponse;
import com.pageon.backend.dto.response.UserInfoResponse;
import com.pageon.backend.dto.token.AccessToken;
import com.pageon.backend.dto.token.TokenInfo;
import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.JwtProvider;
import com.pageon.backend.security.PrincipalUser;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;
    private final RoleService roleService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Transactional
    public void signup(SignupRequest request) {
        User user = createUser(request);

        roleService.assignDefaultRole(user);

        userRepository.save(user);

        log.info("이메일 회원가입 성공 email: {}, 닉네임: {}, provider: {}", user.getEmail(), user.getNickname(), user.getOAuthProvider());
    }


    private User createUser(SignupRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        log.info((request.getBirthDate()));
        LocalDate birthDate = LocalDate.parse(request.getBirthDate(), formatter);
        return User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .birthDate(birthDate)
                .gender(Gender.valueOf(request.getGender()))
                .oAuthProvider(OAuthProvider.EMAIL)
                .termsAgreed(request.getTermsAgreed())
                .build();

    }


    public boolean isEmailDuplicate(String email) {
        log.info("이메일 중복 확인");
        return userRepository.existsByEmail(email);
    }

    public boolean isNicknameDuplicate(String nickname) {
        log.info("닉네임 중복 확인");
        return userRepository.existsByNickname(nickname);
    }

    public JwtTokenResponse login(LoginRequest loginDto, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(), loginDto.getPassword()
                )
        );

        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();

        // 로그인 시 토큰 생성
        String accessToken = generateToken(principalUser, response);

        List<String> userRoles = new ArrayList<>();
        for (RoleType roleType : principalUser.getRoleType()) {
            userRoles.add(roleType.toString());
        }

        String targetPath = null;
        if (userRoles.contains("ROLE_CREATOR")) {
            targetPath = "/creators/contents/dashboard";
        } else if (userRoles.contains("ROLE_ADMIN")) {
            targetPath = "/admin/dashboard";
        }


        return new JwtTokenResponse(true, accessToken, principalUser.getUsers().getOAuthProvider(), userRoles, targetPath);

    }

    private String generateToken(PrincipalUser principalUser, HttpServletResponse response) {
        String accessToken = jwtProvider.generateAccessToken(principalUser.getId(), principalUser.getUsername(), principalUser.getRoleType());
        String refreshToken = jwtProvider.generateRefreshToken(principalUser.getUsername(), principalUser.getId());
        if (accessToken == null || refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_GENERATION_FAILED);
        }

        TokenInfo tokenInfo = new TokenInfo().updateTokenInfo(principalUser.getId(), principalUser.getUsername(), refreshToken);
        String redisKey = "user:auth-info:" + principalUser.getId();

        saveRefreshTokenInRedis(redisKey, tokenInfo);

        jwtProvider.sendTokens(response, accessToken, refreshToken);


        return accessToken;
    }

    private void saveRefreshTokenInRedis(String redisKey, TokenInfo tokenInfo) {
        try {
            redisTemplate.opsForValue().set(redisKey, tokenInfo, Duration.ofDays(180));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }
    }

    public void logout(PrincipalUser principalUser, HttpServletRequest request, HttpServletResponse response) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principalUser.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        String redisKey = "user:auth-info:" + principalUser.getId();

        deleteToken(redisKey, getRefreshToken(request));

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    private void deleteToken(String redisKey, String refreshToken) {

        TokenInfo tokenInfo = (TokenInfo) redisTemplate.opsForValue().get(redisKey);
        if (tokenInfo == null) return;

        if (!tokenInfo.getRefreshToken().equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        redisTemplate.delete(refreshToken);
    }

    @Transactional
    public Map<String, String> passwordFind(FindPasswordRequest passwordDto) {
        Map<String, String> result = new HashMap<>();
        Optional<User> optionalUsers = userRepository.findByEmailAndDeletedAtIsNull(passwordDto.getEmail());
        if (optionalUsers.isPresent()) {
            User user = optionalUsers.get();
            if (user.getOAuthProvider() == OAuthProvider.EMAIL) {
                // provider가 email일 때, 임시 비밀번호 생성 후 db에 저장
                String tempPassword = generateRandomPassword();
                user.updatePassword(passwordEncoder.encode(tempPassword));
                // 임시 비밀번호를 담은 메일 발송
                mailService.sendTemporaryPassword(user.getEmail(), tempPassword);
                result.put("type", "email");
                result.put("message", "임시 비밀번호가 메일로 발송되었습니다.");
            } else {
                result.put("type", "social");
                result.put("message", String.format("%s로 회원가입된 이메일입니다.", user.getOAuthProvider()));
            }
        } else {
            result.put("type", "noUser");
            result.put("message", "회원가입되지 않은 이메일입니다.");
        }

        return result;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String unicodeChars = "!@#$%^&-";
        String numberChars = "0123456789";

        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        sb.append(unicodeChars.charAt(random.nextInt(unicodeChars.length())));
        sb.append(numberChars.charAt(random.nextInt(numberChars.length())));

        return sb.toString();
    }

    public UserInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        return UserInfoResponse.fromEntity(user);
    }

    public boolean checkPassword(Long id, String password) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        return passwordEncoder.matches(password, user.getPassword());
    }

    @Transactional
    public void updateProfile(Long id, UserUpdateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            user.updateNickname(request.getNickname());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.updatePassword(validatePassword(request));
        }
    }

    private String validatePassword(UserUpdateRequest request) {
        if (!request.getPassword().matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#\\-?$%&^])[a-zA-Z0-9!@#\\-?$%&^]{8,}$")) {
            throw new CustomException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }

        return passwordEncoder.encode(request.getPassword());
    }

    @Transactional
    public Map<String, Object> deleteAccount(Long id, UserDeleteRequest userDeleteRequest, HttpServletRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.getOAuthProvider() == OAuthProvider.EMAIL) {

            return deleteEmailAccount(user, userDeleteRequest.getPassword(), request);
        } else {

            return deleteSocialAccount(user, request);
        }

    }

    // provider가 email일 때 계정 삭제 메소드
    private Map<String, Object> deleteEmailAccount(User user, String password, HttpServletRequest request) {
        log.info("이메일 계정 삭제");
        if (passwordEncoder.matches(password, user.getPassword())) {
            // 회원 탈퇴
            return softDeleteAccount(user, "계정이 삭제되었습니다.", null, request);
        } else {
            // 에러 메세지 전송
            return Map.of(
                    "isDeleted", false,
                    "message", "비밀번호가 일치하지 않습니다."
            );
        }
    }

    // provider가 email이 아닐때 즉, 소셜로그인일 때 계정 삭제 메소드
    private Map<String, Object> deleteSocialAccount(User user, HttpServletRequest request) {
        log.info("소셜 계정 삭제");
        String redisKey = String.format("user:oauth:token:%s:%d", user.getOAuthProvider().toString(), user.getId());
        AccessToken accessToken = (AccessToken) redisTemplate.opsForValue().get(redisKey);

        if (accessToken == null) {
            throw new CustomException(ErrorCode.OAUTH_ACCESS_TOKEN_NOT_FOUND);
        }
        switch (user.getOAuthProvider()) {
            case KAKAO -> {
                unlinkKakao(accessToken.getAccessToken());
                return softDeleteAccount(user, "카카오 계정이 삭제되었습니다.", redisKey, request);
            }
            case NAVER -> {
                unlinkNaver(accessToken.getAccessToken());
                return softDeleteAccount(user, "네이버 계정이 삭제되었습니다.", redisKey, request);
            }
            case GOOGLE -> {
                unlinkGoogle(accessToken.getAccessToken());
                return softDeleteAccount(user, "구글 계정이 삭제되었습니다.", redisKey, request);
            }
            default -> throw new CustomException(ErrorCode.INVALID_PROVIDER_TYPE);
        }
    }

    // 삭제 계정 DB 변경
    private Map<String, Object> softDeleteAccount(User user, String message, String redisKey, HttpServletRequest request) {
        // 회원 탈퇴
        user.deleteEmail(String.format("delete_%s_%d", user.getEmail(), user.getId()));
        user.updateNickname(String.format("delete_%s_%d", user.getNickname(), user.getId()));

        // 본인인증 데이터 제거
        user.updateIdentityVerification(null, null, null, null, null, false, null);
        if (user.getOAuthProvider() != OAuthProvider.EMAIL) {
            user.deleteProviderId(String.format("delete_%s_%d", user.getProviderId(), user.getId()));
            // 소셜로그인의 access token 삭제
            redisTemplate.delete(redisKey);
        }
        user.delete();

        // 로그인 시 받은 refresh token 삭제
        String tokenInfoRedisKey = "user:auth-info:" + user.getId();
        deleteToken(tokenInfoRedisKey, getRefreshToken(request));
        return Map.of(
                "isDeleted", true,
                "message", message
        );
    }

    // 카카오 연결 끊기
    private void unlinkKakao(String accessToken) {
        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        sendUnlinkRequest(url, headers, "카카오");
    }

    // 네이버 연결 끊기
    private void unlinkNaver(String accessToken) {
        String url = String.format("https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=%s&client_secret=%s&access_token=%s&service_provider=NAVER",
                naverClientId, naverClientSecret, accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        sendUnlinkRequest(url, headers, "네이버");
    }

    // 구글 연결 끊기
    private void unlinkGoogle(String accessToken) {
        String url = String.format("https://oauth2.googleapis.com/revoke?token=%s", accessToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        sendUnlinkRequest(url, headers, "구글");
    }

    private void sendUnlinkRequest(String url, HttpHeaders headers, String provider) {
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new CustomException(ErrorCode.OAUTH_UNLINK_FAILED);
        }
    }

    public boolean checkIdentityVerification(Long userId) {
        if (userRepository.existsByIdAndIsPhoneVerifiedTrue(userId)) {
            return true;
        }

        throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED_TO_REGISTER_AS_CREATOR);
    }

    public Integer getMyPoint(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        return user.getPointBalance();
    }
}
