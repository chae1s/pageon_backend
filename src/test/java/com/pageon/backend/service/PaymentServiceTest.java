package com.pageon.backend.service;

import com.pageon.backend.client.payment.TossPaymentClient;
import com.pageon.backend.common.enums.TransactionStatus;
import com.pageon.backend.common.enums.TransactionType;
import com.pageon.backend.dto.record.PaymentCache;
import com.pageon.backend.dto.record.TossCancel;
import com.pageon.backend.dto.record.TossConfirm;
import com.pageon.backend.dto.request.PaymentRequest;
import com.pageon.backend.dto.response.PaymentResponse;
import com.pageon.backend.entity.PointTransaction;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.PointTransactionRepository;
import com.pageon.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("PaymentService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private IdempotentService idempotentService;
    @Mock
    private TossPaymentClient tossPaymentClient;
    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private User mockUser() {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(1L);
        lenient().when(user.getPointBalance()).thenReturn(1000);
        return user;
    }

    private PaymentCache mockPaymentCache(TransactionStatus status, Integer amount) {
        PaymentCache cache = mock(PaymentCache.class);
        PointTransaction transaction = mockTransaction(status, amount, null);
        lenient().when(cache.toEntity(any())).thenReturn(transaction);
        return cache;
    }

    private PointTransaction mockTransaction(TransactionStatus status, Integer amount, LocalDateTime paidAt) {
        PointTransaction transaction = mock(PointTransaction.class);
        lenient().when(transaction.getTransactionStatus()).thenReturn(status);
        lenient().when(transaction.getAmount()).thenReturn(amount);
        lenient().when(transaction.getPaidAt()).thenReturn(paidAt);
        lenient().when(transaction.getPaymentKey()).thenReturn("paymentKey");

        return transaction;
    }

    @Test
    @DisplayName("customerKey가 있는 유저는 결제 준비 시 customerKey 재발급 안 함")
    void readyPayment_withExistingCustomerKey_shouldNotReassignCustomerKey() {
        // given
        User user = mockUser();
        when(user.getCustomerKey()).thenReturn("customerKey");

        String orderId = "ORD_orderId";
        PaymentRequest.Ready request = new PaymentRequest.Ready(10000, 10000, "10000P 충전");


        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        doNothing().when(idempotentService).isValidIdempotent(any());

        // when
        PaymentResponse.Ready result = paymentService.readyPayment(1L, request);

        // then
        assertEquals(10000, result.getAmount());
        assertEquals("customerKey", result.getCustomerKey());
        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("ORD_"));
        verify(user, never()).assignCustomerKey(any());
    }

    @Test
    @DisplayName("customerKey가 없는 유저는 결제 준비 시 customerKey 발급")
    void readyPayment_withNullCustomerKey_shouldAssignCustomerKey() {
        // given
        User user = mockUser();
        when(user.getCustomerKey()).thenReturn(null);

        PaymentRequest.Ready request = new PaymentRequest.Ready(10000, 10000, "10000P 충전");

        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        doNothing().when(idempotentService).isValidIdempotent(any());

        // when
        PaymentResponse.Ready result = paymentService.readyPayment(1L, request);

        // then
        verify(user).assignCustomerKey(argThat(key -> key.startsWith("CUSK_")));
        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("ORD_"));
        assertEquals(10000, result.getAmount());
    }

    @Test
    @DisplayName("Redis에 결제 정보 저장 성공")
    void readyPayment_shouldSavePointTransactionInRedis() {
        // given
        User user = mockUser();
        when(user.getCustomerKey()).thenReturn("CUSK_existing");

        PaymentRequest.Ready request = new PaymentRequest.Ready(10000, 10000, "10000P 충전");

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));

        // when
        paymentService.readyPayment(1L, request);

        // then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());

        assertTrue(keyCaptor.getValue().startsWith("point:payment:1:"));
        assertInstanceOf(PaymentCache.class, valueCaptor.getValue());
        assertEquals(Duration.ofMinutes(30), durationCaptor.getValue());
    }

    @Test
    @DisplayName("존재하지 않는 유저면 CustomException 발생")
    void readyPayment_withInvalidUser_shouldThrowException() {
        // given
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        PaymentRequest.Ready request = new PaymentRequest.Ready(10000, 10000, "10000P 충전");
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.readyPayment(1L, request)
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        verify(redisTemplate, never()).opsForValue();
    }
    
    @Test
    @DisplayName("결제 확인 성공")
    void confirmPayment_withValidRequest_shouldCompletePayment() {
        // given
        User user = mockUser();
        String orderId = "ORD_orderId123";
        PaymentCache cache = mockPaymentCache(TransactionStatus.PENDING, 10000);
        PointTransaction transaction = cache.toEntity(user);
        PaymentRequest.Confirm request = new PaymentRequest.Confirm(10000, "paymentKey", orderId);

        String redisKey = String.format("point:payment:%d:%s", 1L, orderId);
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));

        when(valueOperations.getAndDelete(redisKey)).thenReturn(cache);

        TossConfirm confirm = new TossConfirm("mid", "카드", "2025-03-19T00:00:00+09:00", null);
        when(tossPaymentClient.confirmPaymentConnection(request)).thenReturn(confirm);


        //when
        paymentService.confirmPayment(1L, request);
        
        // then
        verify(user).changePoints(10000);
        verify(transaction).completedPayment(any(), anyInt(), eq("paymentKey"), eq("카드"));
        verify(pointTransactionRepository).save(transaction);

    }

    @Test
    @DisplayName("존재하지 않는 유저면 CustomException 발생")
    void confirmPayment_withInvalidUser_shouldThrowException() {
        // given
        String orderId = "ORD_orderId123";
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        PaymentRequest.Confirm request = new PaymentRequest.Confirm(10000, "paymentKey", orderId);
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(1L, request)
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Redis에 결제 정보가 없으면 CustomException 발생")
    void confirmPayment_whenNoCache_shouldThrowCustomException() {
        // given
        User user = mockUser();
        String orderId = "ORD_orderId123";
        PaymentRequest.Confirm request = new PaymentRequest.Confirm(10000, "paymentKey", orderId);

        String redisKey = String.format("point:payment:%d:%s", 1L, orderId);
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(valueOperations.getAndDelete(redisKey)).thenReturn(null);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(1L, request)
        );

        // then
        assertEquals(ErrorCode.POINT_TRANSACTION_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("결제 내역을 찾을 수 없습니다.", exception.getErrorMessage());
        verify(tossPaymentClient, never()).confirmPaymentConnection(request);

    }

    @Test
    @DisplayName("이미 처리된 결제면 CustomException 발생")
    void confirmPayment_whenAlreadyConfirmed_shouldThrowCustomException() {
        // given
        User user = mockUser();
        String orderId = "ORD_orderId123";
        PaymentCache cache = mockPaymentCache(TransactionStatus.COMPLETED, 10000);
        PointTransaction transaction = cache.toEntity(user);
        PaymentRequest.Confirm request = new PaymentRequest.Confirm(10000, "paymentKey", orderId);

        String redisKey = String.format("point:payment:%d:%s", 1L, orderId);
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(valueOperations.getAndDelete(redisKey)).thenReturn(cache);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(1L, request)
        );

        // then
        assertEquals(ErrorCode.ALREADY_PAYMENT_CONFIRM, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("이미 처리된 결제입니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("금액 불일치 시 failedPayment 저장 후 CustomException 발생")
    void confirmPayment_whenNotMatchAmount_shouldSaveFailedAndThrowCustomException() {
        // given
        User user = mockUser();
        String orderId = "ORD_orderId123";
        PaymentCache cache = mockPaymentCache(TransactionStatus.PENDING, 50000);
        PointTransaction transaction = cache.toEntity(user);
        PaymentRequest.Confirm request = new PaymentRequest.Confirm(10000, "paymentKey", orderId);

        String redisKey = String.format("point:payment:%d:%s", 1L, orderId);
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(valueOperations.getAndDelete(redisKey)).thenReturn(cache);

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(1L, request)
        );

        // then
        verify(transaction).failedPayment();
        verify(tossPaymentClient, never()).confirmPaymentConnection(request);
        verify(pointTransactionRepository).save(transaction);
        assertEquals(ErrorCode.AMOUNT_NOT_MATCH, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("결제 금액이 일치하지 않습니다.", exception.getErrorMessage());

    }

    @Test
    @DisplayName("토스 API 실패 시 failedPayment 저장 후 CustomException 발생")
    void confirmPayment_whenApiCallFailed_shouldSaveFailedAndThrowCustomException() {
        // given
        User user = mockUser();
        String orderId = "ORD_orderId123";
        PaymentCache cache = mockPaymentCache(TransactionStatus.PENDING, 10000);
        PointTransaction transaction = cache.toEntity(user);
        PaymentRequest.Confirm request = new PaymentRequest.Confirm(10000, "paymentKey", orderId);

        String redisKey = String.format("point:payment:%d:%s", 1L, orderId);
        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(valueOperations.getAndDelete(redisKey)).thenReturn(cache);
        when(tossPaymentClient.confirmPaymentConnection(any())).thenThrow(new RuntimeException("API 연결 실패"));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.confirmPayment(1L, request)
        );

        // then
        assertEquals(ErrorCode.PAYMENT_API_FAILED, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("결제 API 호출에 실패했습니다.", exception.getErrorMessage());
        verify(transaction).failedPayment();
        verify(pointTransactionRepository).save(transaction);
        verify(user, never()).changePoints(10000);

    }

    @Test
    @DisplayName("결제 취소 성공")
    void cancelPayment_whenValidTransaction_shouldCompleteCancel() {
        // given
        User user = mockUser();

        PointTransaction transaction = mockTransaction(TransactionStatus.COMPLETED, 100, LocalDateTime.now().minusDays(1));

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.of(transaction));

        TossCancel cancel = new TossCancel("mid", "CANCELED", "2025-03-19T00:00:00+09:00");
        when(tossPaymentClient.cancelPaymentConnection("paymentKey")).thenReturn(cancel);

        //when
        paymentService.cancelPayment(1L, 1L);

        // then
        verify(user).changePoints(-100);
        verify(transaction).cancelPayment(any(LocalDateTime.class));
        verify(tossPaymentClient).cancelPaymentConnection(eq("paymentKey"));

    }

    @Test
    @DisplayName("존재하지 않는 유저면 CustomException 발생")
    void cancelPayment_withInvalidUser_shouldThrowException() {
        // given

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("존재하지 않는 사용자입니다.", exception.getErrorMessage());
        verify(tossPaymentClient, never()).cancelPaymentConnection("paymentKey");
    }

    @Test
    @DisplayName("존재하지 않는 거래 내역이면 CustomException 발생")
    void cancelPayment_withInvalidPointTransaction_shouldThrowCustomException() {
        // given
        User user = mockUser();

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.POINT_TRANSACTION_NOT_FOUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("결제 내역을 찾을 수 없습니다.", exception.getErrorMessage());
        verify(tossPaymentClient, never()).cancelPaymentConnection("paymentKey");
        verify(user, never()).changePoints(-100);

    }

    @Test
    @DisplayName("결제 상태가 Completed가 아니면 CustomException 발생")
    void cancelPayment_whenStatusIsNotCompleted_shouldThrowCustomException() {
        // given
        User user = mockUser();

        PointTransaction transaction = mockTransaction(TransactionStatus.PENDING, 100, LocalDateTime.now().minusDays(1));

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.of(transaction));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.PAYMENT_NOT_COMPLETED, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("완료된 결제만 취소할 수 있습니다.", exception.getErrorMessage());
        verify(tossPaymentClient, never()).cancelPaymentConnection("paymentKey");
        verify(user, never()).changePoints(-100);

    }

    @Test
    @DisplayName("환불할 포인트가 부족하면 CustomException 발생")
    void cancelPayment_whenUserPointsInsufficient_shouldThrowCustomException() {
        // given
        User user = mockUser();

        PointTransaction transaction = mockTransaction(TransactionStatus.COMPLETED, 10000, LocalDateTime.now().minusDays(1));

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.of(transaction));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.INSUFFICIENT_POINTS_FOR_REFUND, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("환불할 포인트 잔액이 부족합니다.", exception.getErrorMessage());
        verify(tossPaymentClient, never()).cancelPaymentConnection("paymentKey");
        verify(user, never()).changePoints(-100);

    }

    @Test
    @DisplayName("환불 가능 기간이 지났으면 CustomException 발생")
    void cancelPayment_whenInvalidPaidAt_shouldThrowCustomException() {
        // given
        User user = mockUser();

        PointTransaction transaction = mockTransaction(TransactionStatus.COMPLETED, 100, LocalDateTime.now().minusDays(10));

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.of(transaction));

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.REFUND_PERIOD_EXPIRED, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("환불 가능 기간이 지났습니다.", exception.getErrorMessage());
        verify(tossPaymentClient, never()).cancelPaymentConnection("paymentKey");
        verify(user, never()).changePoints(-100);

    }

    @Test
    @DisplayName("결제 상태가 canceled가 아니면 CustomException 발생")
    void cancelPayment_whenResultStatusNotCanceled_shouldThrowCustomException() {
        // given
        User user = mockUser();

        PointTransaction transaction = mockTransaction(TransactionStatus.COMPLETED, 100, LocalDateTime.now().minusDays(1));

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.of(transaction));

        TossCancel cancel = new TossCancel("mid", "FAILED", "2025-03-19T00:00:00+09:00");
        when(tossPaymentClient.cancelPaymentConnection("paymentKey")).thenReturn(cancel);

        //when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.REFUND_STATUS_INVALID, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("취소되지 않은 결제입니다.", exception.getErrorMessage());
        verify(user, never()).changePoints(-100);
        verify(transaction, never()).cancelPayment(any(LocalDateTime.class));

    }

    @Test
    @DisplayName("토스 API 실패 시 CustomException 발생")
    void cancelPayment_whenApiCallFailed_shouldThrowCustomException() {
        // given
        User user = mockUser();

        PointTransaction transaction = mockTransaction(TransactionStatus.COMPLETED, 100, LocalDateTime.now().minusDays(1));

        doNothing().when(idempotentService).isValidIdempotent(any());
        when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));
        when(pointTransactionRepository.findByIdAndUserWithLock(1L, 1L)).thenReturn(Optional.of(transaction));

        when(tossPaymentClient.cancelPaymentConnection(any())).thenThrow(new RuntimeException("API 연결 실패"));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> paymentService.cancelPayment(1L, 1L)
        );

        // then
        assertEquals(ErrorCode.REFUND_API_FAILED, ErrorCode.valueOf(exception.getErrorCode()));
        assertEquals("환불 API 호출에 실패했습니다.", exception.getErrorMessage());
        verify(user, never()).changePoints(-100);
        verify(transaction, never()).cancelPayment(any(LocalDateTime.class));

    }


}

