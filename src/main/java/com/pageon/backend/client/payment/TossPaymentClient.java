package com.pageon.backend.client.payment;

import com.pageon.backend.dto.record.BankAccountValidation;
import com.pageon.backend.dto.record.TossCancel;
import com.pageon.backend.dto.record.TossConfirm;
import com.pageon.backend.dto.request.BankAccountVerification;
import com.pageon.backend.dto.request.PaymentRequest;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Component
public class TossPaymentClient {

    private static final String BASIC_AUTH_PREFIX = "Basic ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final RestClient restClient;

    public TossPaymentClient(
            @Value("${payment.toss.api-url}") String apiUrl,
            @Value("${payment.toss.secret-key}") String secretKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(AUTHORIZATION_HEADER, BASIC_AUTH_PREFIX + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public BankAccountValidation validateAccount(BankAccountVerification request) {
        try {
            return restClient.post()
                    .uri("/v2/bank-accounts/verify-holder-real-name")
                    .body(request)
                    .retrieve()
                    .body(BankAccountValidation.class);
        } catch (HttpClientErrorException e) {
            throw new CustomException(ErrorCode.TOSS_CLIENT_ERROR);
        }
    }

    public TossConfirm confirmPaymentConnection(PaymentRequest.Confirm confirm) {
        return withCustomException(() ->
                restClient.post()
                        .uri("/v1/payments/confirm")
                        .body(confirm)
                        .retrieve()
                        .body(TossConfirm.class));
    }

    public TossCancel cancelPaymentConnection(String paymentKey) {
        Map<String, String> reason = Map.of(
                "cancelReason", "구매자가 취소를 원함"
        );

        return withCustomException(() ->
                restClient.post()
                        .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                        .body(reason)
                        .retrieve()
                        .body(TossCancel.class));
    }

    private <T> T withCustomException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException e) {
            log.error("Toss API 4xx 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.TOSS_CLIENT_ERROR);
        } catch (Exception e) {
            log.error("Toss API 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }




}
