package com.pageon.backend.service;

import com.pageon.backend.common.enums.Gender;
import com.pageon.backend.common.enums.IdentityProvider;
import com.pageon.backend.dto.common.IdentityVerificationCustomer;
import com.pageon.backend.dto.payload.OtpVerificationPayload;
import com.pageon.backend.dto.request.IdentityVerificationRequest;
import com.pageon.backend.dto.request.IdentityVerificationResultRequest;
import com.pageon.backend.dto.response.IdentityVerificationIdResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.security.PrincipalUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;
    private final CommonService commonService;

    //  본인인증 요청 식별 위해 identityVerificationId 발급 후 redis에 저장
    public IdentityVerificationIdResponse createIdentityVerificationId(Long userId) {

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.getIsPhoneVerified()) {
            throw new CustomException(ErrorCode.IDENTITY_ALREADY_VERIFIED);
        }

        String identityVerificationId = UUID.randomUUID().toString();
        try {
            redisTemplate.opsForValue().set(String.format("user:verification:%d", user.getId()), identityVerificationId, Duration.ofMinutes(10));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }

        return new IdentityVerificationIdResponse(identityVerificationId);
    }

    public boolean createAndStoreOtp(String identityVerificationId, Long userId, IdentityVerificationRequest identityVerificationRequest) {
        // 로그인한 유저의 이메일로 db 검색
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (!identityVerificationRequest.getMethod().equals("SMS"))
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_METHOD);


        if (user.getPhoneNumber() != null && user.getPhoneNumber().equals(identityVerificationRequest.getCustomer().getPhoneNumber())) {
            throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_VERIFIED);
        }

        checkIdentityVerificationId(identityVerificationId, user.getId());

        SecureRandom random = new SecureRandom();
        String otp = String.valueOf(random.nextInt(900000) + 100000);

        OtpVerificationPayload otpVerificationPayload = new OtpVerificationPayload(otp, identityVerificationRequest.getCustomer());
        String redisKey = String.format("user:verification:%d:%s", user.getId(), identityVerificationId);
        try {
            redisTemplate.opsForValue().set(redisKey, otpVerificationPayload, Duration.ofMinutes(3));
            log.info("본인인증 번호 redis 저장");
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }

        // 문자 전송 메소드 추가
        return smsService.singleMessageSend(otpVerificationPayload);

    }

    @Transactional
    public boolean verifyOtpAndUpdateUser(String identityVerificationId, Long userId, IdentityVerificationResultRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        checkIdentityVerificationId(identityVerificationId, user.getId());

        OtpVerificationPayload otpVerificationPayload;
        String redisKey = String.format("user:verification:%d:%s", user.getId(), identityVerificationId);
        try {
            otpVerificationPayload = (OtpVerificationPayload) redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }

        if (otpVerificationPayload == null)
            throw new CustomException(ErrorCode.OTP_PAYLOAD_NOT_FOUND);

        if (!request.getOtp().equals(otpVerificationPayload.getOtp()))
            throw new CustomException(ErrorCode.OTP_NOT_MATCH);

        IdentityVerificationCustomer customer = otpVerificationPayload.getCustomer();

        Map<String, Object> identityNumInfo = parseIdentityNumber(customer.getIdentityNumber());


        user.updateIdentityVerification(
                customer.getName(), customer.getPhoneNumber(),
                (LocalDate) identityNumInfo.get("birthDate"), Gender.valueOf((String) identityNumInfo.get("gender")),
                request.getDi(), true, IdentityProvider.valueOf(request.getIdentityProvider())
        );

        try {
            redisTemplate.delete(redisKey);
            redisTemplate.delete(String.format("user:verification:%d", user.getId()));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }

        return true;
    }

    // redis에 저장된 identityVerificationId와 url로 넘어온 identityVerificationId를 비교하는 메서드
    private void checkIdentityVerificationId(String identityVerificationId, Long userId) {
        String storedVerificationId;
        try {
             storedVerificationId = (String) redisTemplate.opsForValue().get(String.format("user:verification:%d", userId));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }

        if (!identityVerificationId.equals(storedVerificationId))
            throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_ID_NOT_MATCH);
    }

    private Map<String, Object> parseIdentityNumber(String identityNumber) {
        if (identityNumber == null || !identityNumber.matches("\\d{7}")) {
            throw new CustomException(ErrorCode.INVALID_IDENTITY_NUMBER);
        }
        String birthDate = identityNumber.substring(0, 6);
        int genderNum = Integer.parseInt(identityNumber.substring(6));

        String yearPart = birthDate.substring(0, 2);
        String monthPart = birthDate.substring(2, 4);
        String dayPart = birthDate.substring(4);

        int year = Integer.parseInt(yearPart);
        int currentYear = LocalDate.now().getYear() % 100;

        int fullYear = (year <= currentYear) ? 2000 + year : 1900 + year;

        String gender = (genderNum % 2 == 0) ? "FEMALE" : "MALE";

        return Map.of(
                "birthDate", LocalDate.of(fullYear, Integer.parseInt(monthPart), Integer.parseInt(dayPart)),
                "gender", gender
        );

    }
}
