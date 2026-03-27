package com.pageon.backend.controller;

import com.pageon.backend.dto.request.ContentRequest;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators/contents")
@RequiredArgsConstructor
public class CreatorContentController {

    private final CreatorContentService creatorContentService;

    @PostMapping
    public ResponseEntity<Void> createContent(
            @AuthenticationPrincipal PrincipalUser principalUser, @ModelAttribute ContentRequest.Create request
    ) {

        creatorContentService.createContent(principalUser.getId(), request);

        return ResponseEntity.ok().build();
    }

}
