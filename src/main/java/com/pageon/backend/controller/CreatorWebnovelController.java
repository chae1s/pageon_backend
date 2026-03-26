package com.pageon.backend.controller;

import com.pageon.backend.dto.request.ContentCreateRequest;
import com.pageon.backend.dto.request.ContentDeleteRequest;
import com.pageon.backend.dto.request.ContentUpdateRequest;
import com.pageon.backend.dto.response.CreatorContentListResponse;
import com.pageon.backend.dto.response.CreatorWebnovelResponse;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorWebnovelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/creators/webnovels")
@RequiredArgsConstructor
public class CreatorWebnovelController {

    private final CreatorWebnovelService creatorWebnovelService;

    @PostMapping()
    public ResponseEntity<Void> createContent(@AuthenticationPrincipal PrincipalUser principalUser, @Valid @ModelAttribute ContentCreateRequest contentCreateRequest) {
        creatorWebnovelService.createContent(principalUser, contentCreateRequest);

        return ResponseEntity.ok().build();
    }

//    @GetMapping("/{webnovelId}")
//    public ResponseEntity<CreatorWebnovelResponse> getContentById(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long webnovelId) {
//
//        return ResponseEntity.ok(creatorWebnovelService.getContentById(principalUser, webnovelId));
//    }

    @GetMapping()
    public ResponseEntity<List<CreatorContentListResponse>> getMyContents(@AuthenticationPrincipal PrincipalUser principalUser) {

        return ResponseEntity.ok(creatorWebnovelService.getMyContents(principalUser));
    }

    @PatchMapping("/{webnovelId}")
    public ResponseEntity<Long> updateContent(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long webnovelId, @Valid @ModelAttribute ContentUpdateRequest contentUpdateRequest) {

        return ResponseEntity.ok(creatorWebnovelService.updateContent(principalUser, webnovelId, contentUpdateRequest));
    }

    @PostMapping("/{webnovelId}/delete-request")
    public ResponseEntity<Void> deleteRequestContent(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long webnovelId, @RequestBody ContentDeleteRequest contentDeleteRequest) {
        creatorWebnovelService.deleteRequestContent(principalUser, webnovelId, contentDeleteRequest);

        return ResponseEntity.ok().build();
    }



}
