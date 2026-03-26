package com.pageon.backend.controller;

import com.pageon.backend.dto.request.IdentityVerificationRequest;
import com.pageon.backend.dto.request.IdentityVerificationResultRequest;
import com.pageon.backend.dto.response.IdentityVerificationIdResponse;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.PortOneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PortOneController {

    private final PortOneService portOneService;

    @PostMapping("/api/identity-verifications")
    public ResponseEntity<IdentityVerificationIdResponse> createIdentityVerificationId(@AuthenticationPrincipal PrincipalUser principalUser) {

        return ResponseEntity.ok(portOneService.createIdentityVerificationId(principalUser.getId()));
    }

    @PostMapping("/api/identity-verifications/{identityVerificationId}/send")
    public ResponseEntity<Boolean> createAndStoreOtp(
            @PathVariable(name = "identityVerificationId") String identityVerificationId,
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestBody IdentityVerificationRequest request
    ) {

        return ResponseEntity.ok(portOneService.createAndStoreOtp(identityVerificationId, principalUser.getId(), request));
    }

    @PostMapping("/api/identity-verifications/{identityVerificationId}/confirm")
    public ResponseEntity<Boolean> verifyOtpAndUpdateUser(
            @PathVariable(name = "identityVerificationId") String identityVerificationId,
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestBody IdentityVerificationResultRequest request
    ) {

        return ResponseEntity.ok(portOneService.verifyOtpAndUpdateUser(identityVerificationId, principalUser.getId(), request));
    }

}
