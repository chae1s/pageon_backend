package com.pageon.backend.service;

import com.pageon.backend.dto.common.IdentityVerificationCustomer;
import com.pageon.backend.dto.payload.OtpVerificationPayload;
import com.pageon.backend.dto.request.IdentityVerificationRequest;
import com.pageon.backend.dto.request.IdentityVerificationResultRequest;
import com.pageon.backend.dto.response.IdentityVerificationIdResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("PortOneService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PortOneServiceTest {
    @InjectMocks
    private PortOneService portOneService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private UserRepository userRepository;
    @Mock
    SmsService smsService;

    private User defaultUser() {
        return User.builder()
                .id(1L)
                .email("test@mail.com")
                .isPhoneVerified(false)
                .build();
    }

    @Test
    @DisplayName("본인인증 확인 ID 발급")
    void createIdentityVerificationId_whenValidUser_shouldReturnIdResponse() {
        // given
        Long userId = 1L;


        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(defaultUser()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        //when
        IdentityVerificationIdResponse result = portOneService.createIdentityVerificationId(userId);

        // then
        assertNull(result.getIdentityVerificationId());
        verify(valueOperations).set(
                eq("user:verification:1"),
                anyString(),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    @DisplayName("로그인한 유저가 이미 본인인증을 완료했을 경우 CustomException 발생")
    void createIdentityVerificationId_whenIsPhoneVerifiedTrue_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .password("password")
                .isPhoneVerified(true)
                .build();

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            portOneService.createIdentityVerificationId(userId);
        });

        // then
        assertEquals("이미 본인인증을 완료한 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.IDENTITY_ALREADY_VERIFIED, ErrorCode.valueOf(exception.getErrorCode()));
    }

    @Test
    @DisplayName("존재하지 않은 유저면 CustomException 발생")
    void createIdentityVerificationId_whenInvalidUser_shouldThrowCustomException() {
        // given
        Long userId = 1L;

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            portOneService.createIdentityVerificationId(userId);
        });

        // then
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));

    }
    
    @Test
    @DisplayName("OTP 생성 및 redis에 저장 성공")
    void createAndStoreOtp_withValidUserAndValidInput_shouldCreateOtpAndSaveRedis() {
        // given
        Long userId = 1L;
        String identityVerificationId = "test-verification-id";
        IdentityVerificationCustomer customer = new IdentityVerificationCustomer("박누구", "010-1111-1111", "9604032");
        IdentityVerificationRequest request = new IdentityVerificationRequest(customer, "SMS");

        String idRedisKey = String.format("user:verification:%d", userId);
        String otpRedisKey = String.format("user:verification:%d:%s", userId, identityVerificationId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(defaultUser()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(idRedisKey)).thenReturn(identityVerificationId);

        when(smsService.singleMessageSend(any())).thenReturn(true);

        //when
        boolean result = portOneService.createAndStoreOtp(identityVerificationId, userId, request);
        
        // then
        assertTrue(result);
        ArgumentCaptor<OtpVerificationPayload> otpCaptor = ArgumentCaptor.forClass(OtpVerificationPayload.class);

        verify(valueOperations).set(eq(otpRedisKey), otpCaptor.capture(), eq(Duration.ofMinutes(3)));

        OtpVerificationPayload otpPayload = otpCaptor.getValue();
        assertNull(otpPayload.getOtp());
        assertEquals(6, otpPayload.getOtp().length());
        assertTrue(otpPayload.getOtp().matches("\\d{6}"));

        ArgumentCaptor<OtpVerificationPayload> smsCaptor =
                ArgumentCaptor.forClass(OtpVerificationPayload.class);
        verify(smsService).singleMessageSend(smsCaptor.capture());
        assertEquals(otpPayload.getOtp(), smsCaptor.getValue().getOtp(),
                "Redis에 저장된 OTP와 SMS로 전달된 OTP가 동일해야 합니다.");
    }

    @Test
    @DisplayName("입력한 전화번호가 이미 인증이 완료된 상태라면 CustomException 발생")
    void createAndStoreOtp_whenAlreadyVerifiedPhoneNumber_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String identityVerificationId = "test-verification-id";

        String phoneNumber = "01011111111";
        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .phoneNumber(phoneNumber)
                .isPhoneVerified(true)
                .build();

        IdentityVerificationCustomer customer = new IdentityVerificationCustomer("박누구", phoneNumber, "9604032");
        IdentityVerificationRequest request = new IdentityVerificationRequest(customer, "SMS");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            portOneService.createAndStoreOtp(identityVerificationId, userId, request);
        });

        // then
        assertEquals("해당 전화번호는 이미 본인인증에 사용되었습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.PHONE_NUMBER_ALREADY_VERIFIED, ErrorCode.valueOf(exception.getErrorCode()));


    }
    
    @Test
    @DisplayName("전달받은 method가 SMS이 아닌 경우 CustomException 발생")
    void createAndStoreOtp_withInvalidMethod_shouldThrowException() {
        // given
        Long userId = 1L;
        String identityVerificationId = "test-verification-id";

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(defaultUser()));

        IdentityVerificationCustomer customer = new IdentityVerificationCustomer("박누구", "010-1111-1111", "9604032");
        IdentityVerificationRequest request = new IdentityVerificationRequest(customer, "NULL");
        
        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
           portOneService.createAndStoreOtp(identityVerificationId, userId, request);
        });
        
        // then
        assertEquals("지원하지 않는 본인인증 방식입니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.INVALID_VERIFICATION_METHOD, ErrorCode.valueOf(exception.getErrorCode()));
    }
    
    @Test
    @DisplayName("전달받은 identityVerificationId와 redis에 저장된 identityVerificationId가 다르면 CustomException 발생")
    void createAndStoreOtp_withMissmatchIdentityVerificationId_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String identityVerificationId = "test-verification-id";
        String redisVerificationId = "redis-verification-id";

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(defaultUser()));
        String idRedisKey = String.format("user:verification:%d", userId);

        IdentityVerificationCustomer customer = new IdentityVerificationCustomer("박누구", "010-1111-1111", "9604032");
        IdentityVerificationRequest request = new IdentityVerificationRequest(customer, "SMS");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(idRedisKey)).thenReturn(redisVerificationId);

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            portOneService.createAndStoreOtp(identityVerificationId, userId, request);
        });
        
        // then
        assertEquals("전달된 인증 ID가 일치하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.IDENTITY_VERIFICATION_ID_NOT_MATCH, ErrorCode.valueOf(exception.getErrorCode()));
    }
    
    @Test
    @DisplayName("받아온 otp가 일치할 때 user DB Update")
    void verifyOtpAndUpdateUser_withMatchOtp_shouldUpdateUser() {
        // given
        Long userId = 1L;
        String sendOtp = "493029";
        String identityVerificationId = "identity-verification-id";
        String redisOtp = "493029";
        User user = defaultUser();

        IdentityVerificationCustomer customer = new IdentityVerificationCustomer("박누구", "010-1111-1111", "9604032");

        OtpVerificationPayload payload = new OtpVerificationPayload(redisOtp, customer);
        String idRedisKey = String.format("user:verification:%d", userId);
        String otpRedisKey = String.format("user:verification:%d:%s", userId, identityVerificationId);

        String di = UUID.randomUUID().toString();
        IdentityVerificationResultRequest request = new IdentityVerificationResultRequest(di, sendOtp, "DANAL");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(idRedisKey)).thenReturn(identityVerificationId);
        when(valueOperations.get(otpRedisKey)).thenReturn(payload);

        //when
        portOneService.verifyOtpAndUpdateUser(identityVerificationId, userId, request);

        // then
        assertEquals("박누구", user.getName());
        assertEquals("010-1111-1111", user.getPhoneNumber());
        assertTrue(user.getIsPhoneVerified());

        verify(redisTemplate).delete(otpRedisKey);
        verify(redisTemplate).delete(idRedisKey);

    }

    
    @Test
    @DisplayName("identityVerificationId로 redis에서 꺼낸 데이터가 없을 경우 CustomException 발생")
    void verifyOtpAndUpdateUser_withNullStoredData_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String identityVerificationId = "identity-verification-id";

        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .build();

        String idRedisKey = String.format("user:verification:%d", userId);
        String otpRedisKey = String.format("user:verification:%d:%s", userId, identityVerificationId);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(idRedisKey)).thenReturn(identityVerificationId);
        when(valueOperations.get(otpRedisKey)).thenReturn(null);

        IdentityVerificationResultRequest request
                = new IdentityVerificationResultRequest(UUID.randomUUID().toString(), "000000", "DANAL");
        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> portOneService.verifyOtpAndUpdateUser(identityVerificationId, userId, request)
        );
        
        // then
        assertEquals("OTP 정보가 존재하지 않거나 만료되었습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.OTP_PAYLOAD_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        
    }

    @Test
    @DisplayName("프론트에서 받은 otp 번호가 redis에 저장된 otp 번호와 일치하지 않을 때 CustomException 발생")
    void verifyOtpAndUpdateUser_withMissmatchOtp_shouldThrowCustomException() {
        // given
        Long userId = 1L;
        String sendOtp = "392059";
        String identityVerificationId = "identity-verification-id";
        String redisOtp = "493029";


        User user = User.builder()
                .id(userId)
                .email("test@mail.com")
                .build();

        IdentityVerificationCustomer customer = new IdentityVerificationCustomer("박누구", "010-1111-1111", "9604032");
        OtpVerificationPayload payload = new OtpVerificationPayload(redisOtp, customer);

        String idRedisKey = String.format("user:verification:%d", userId);
        String otpRedisKey = String.format("user:verification:%d:%s", userId, identityVerificationId);

        String di = UUID.randomUUID().toString();
        IdentityVerificationResultRequest request = new IdentityVerificationResultRequest(di, sendOtp, "DANAL");

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(idRedisKey)).thenReturn(identityVerificationId);
        when(valueOperations.get(otpRedisKey)).thenReturn(payload);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> portOneService.verifyOtpAndUpdateUser(identityVerificationId, userId, request)
        );

        // then
        assertEquals("전달된 OTP가 일치하지 않습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.OTP_NOT_MATCH, ErrorCode.valueOf(exception.getErrorCode()));

    }


}