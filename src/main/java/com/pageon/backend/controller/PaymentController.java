package com.pageon.backend.controller;

import com.pageon.backend.dto.request.PaymentRequest;
import com.pageon.backend.dto.response.PaymentResponse;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/ready")
    public ResponseEntity<PaymentResponse.Ready> readyPayment(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestBody PaymentRequest.Ready request
    ) {

        PaymentResponse.Ready response = paymentService.readyPayment(principalUser.getId(), request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmPayment(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestBody PaymentRequest.Confirm request
    ) {
        paymentService.confirmPayment(principalUser.getId(), request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{transactionId}")
    public ResponseEntity<Void> cancelPayment(
            @AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long transactionId
    ) {
        paymentService.cancelPayment(principalUser.getId(), transactionId);

        return ResponseEntity.ok().build();
    }



}
