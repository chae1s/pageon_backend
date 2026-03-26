package com.pageon.backend.controller;

import com.pageon.backend.dto.request.TempCodeRequest;
import com.pageon.backend.dto.response.JwtTokenResponse;
import com.pageon.backend.dto.response.ReissuedTokenResponse;
import com.pageon.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(("/api/auth"))
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<ReissuedTokenResponse> reissueToken(HttpServletRequest request, HttpServletResponse response) {

        return ResponseEntity.ok(authService.reissueToken(request, response));
    }

    @PostMapping("/exchange")
    public ResponseEntity<JwtTokenResponse> exchangeToken(HttpServletResponse response, @RequestBody TempCodeRequest tempCodeRequest) {

        log.info("exchangeToken");
        JwtTokenResponse tokenResponse = authService.exchangeCode(response, tempCodeRequest);

        return ResponseEntity.ok(tokenResponse);
    }
}
