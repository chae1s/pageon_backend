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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final UserRepository userRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final IdempotentService idempotentService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TossPaymentClient tossPaymentClient;

    @Transactional
    public PaymentResponse.Ready readyPayment(Long userId, PaymentRequest.Ready request) {

        String[] key = {String.valueOf(userId), "ready",request.getAmount().toString()};
        idempotentService.isValidIdempotent(Arrays.asList(key));

        User user = userRepository.findByIdWithLock(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        if (user.getCustomerKey() == null) {
            String customerKey = "CUSK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            user.assignCustomerKey(customerKey);
        }

        String orderId = "ORD_" + UUID.randomUUID().toString().substring(0, 12);

        PaymentCache paymentCache = PaymentCache.builder()
                .userId(userId)
                .transactionType(TransactionType.CHARGE)
                .transactionStatus(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .point(request.getPoint())
                .description(request.getDescription())
                .orderId(orderId)
                .build();

        String redisKey = String.format("point:payment:%d:%s", userId, orderId);
        try {
            redisTemplate.opsForValue().set(redisKey, paymentCache, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCode.REDIS_CONNECTION_FAILED);
        }
        return PaymentResponse.Ready.builder()
                .orderId(orderId)
                .customerKey(user.getCustomerKey())
                .amount(request.getAmount())
                .build();
    }

    @Transactional
    public void confirmPayment(Long userId, PaymentRequest.Confirm confirm) {

        String[] key = {String.valueOf(userId), confirm.getOrderId(), confirm.getAmount().toString()};
        idempotentService.isValidIdempotent(Arrays.asList(key));

        User user = userRepository.findByIdWithLock(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        String redisKey = String.format("point:payment:%d:%s", userId, confirm.getOrderId());
        PaymentCache paymentCache = (PaymentCache) redisTemplate.opsForValue().getAndDelete(redisKey);

        if (paymentCache == null) {
            throw new CustomException(ErrorCode.POINT_TRANSACTION_NOT_FOUND);
        }

        PointTransaction transaction = paymentCache.toEntity(user);

        if (transaction.getTransactionStatus() != TransactionStatus.PENDING) {
            throw new CustomException(ErrorCode.ALREADY_PAYMENT_CONFIRM);
        }

        if (!transaction.getAmount().equals(confirm.getAmount())) {
            transaction.failedPayment();
            pointTransactionRepository.save(transaction);
            throw new CustomException(ErrorCode.AMOUNT_NOT_MATCH);
        }

        TossConfirm result = confirmConnection(transaction, confirm);

        LocalDateTime paidAt = OffsetDateTime.parse(result.approvedAt()).toLocalDateTime();

        String paymentMethod = formatMethod(result);

        user.changePoints(confirm.getAmount());

        transaction.completedPayment(paidAt, user.getPointBalance(), confirm.getPaymentKey(), paymentMethod);

        pointTransactionRepository.save(transaction);

    }


    private TossConfirm confirmConnection(PointTransaction transaction, PaymentRequest.Confirm confirm) {
        try {

            return tossPaymentClient.confirmPaymentConnection(confirm);

        } catch (Exception e) {
            transaction.failedPayment();
            pointTransactionRepository.save(transaction);
            throw new CustomException(ErrorCode.PAYMENT_API_FAILED);
        }
    }


    private String formatMethod(TossConfirm result) {

        String method = result.method();

        TossConfirm.EasyPay easyPay = result.easyPay();
        if (easyPay != null) {
            return String.format("%s %s", method, easyPay.provider());
        } else {
            return method;
        }
    }

    @Transactional
    public void cancelPayment(Long userId, Long transactionId) {

        String[] key = {String.valueOf(userId), "cancel",transactionId.toString()};
        idempotentService.isValidIdempotent(Arrays.asList(key));

        User user = userRepository.findByIdWithLock(userId).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        PointTransaction transaction = pointTransactionRepository.findByIdAndUserWithLock(transactionId, userId).orElseThrow(
                () -> new CustomException(ErrorCode.POINT_TRANSACTION_NOT_FOUND)
        );

        if (transaction.getTransactionStatus() != TransactionStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_COMPLETED);
        }

        if (user.getPointBalance() <= transaction.getAmount()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_POINTS_FOR_REFUND);
        }

        LocalDateTime limitDate = LocalDateTime.now().minusDays(7);
        if (transaction.getPaidAt().isBefore(limitDate)) {
            throw new CustomException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        TossCancel result = cancelConnection(transaction.getPaymentKey());

        LocalDateTime cancelledAt = OffsetDateTime.parse(result.approvedAt()).toLocalDateTime();

        if (!result.status().equals("CANCELED")) {
            throw new CustomException(ErrorCode.REFUND_STATUS_INVALID);
        }
        user.changePoints(-transaction.getAmount());
        transaction.cancelPayment(cancelledAt);

    }

    private TossCancel cancelConnection(String paymentKey) {

        try {

            return tossPaymentClient.cancelPaymentConnection(paymentKey);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.REFUND_API_FAILED);
        }

    }
}
