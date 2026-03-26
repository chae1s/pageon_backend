package com.pageon.backend.controller;

import com.pageon.backend.dto.request.RegisterCreatorRequest;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorService creatorService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerCreator(@AuthenticationPrincipal PrincipalUser principalUser, @Valid @RequestBody RegisterCreatorRequest request) {
        creatorService.registerCreator(principalUser.getId(), request);

        return ResponseEntity.ok().build();
    }
}
