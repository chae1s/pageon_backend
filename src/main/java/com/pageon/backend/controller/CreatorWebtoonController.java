package com.pageon.backend.controller;

import com.pageon.backend.dto.request.ContentCreateRequest;
import com.pageon.backend.dto.request.ContentDeleteRequest;
import com.pageon.backend.dto.request.ContentUpdateRequest;
import com.pageon.backend.dto.response.CreatorContentListResponse;
import com.pageon.backend.dto.response.CreatorWebtoonResponse;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorWebtoonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/creators/webtoons")
@RequiredArgsConstructor
public class CreatorWebtoonController {

    private final CreatorWebtoonService creatorWebtoonService;

    @PostMapping()
    public ResponseEntity<Void> createContent(@AuthenticationPrincipal PrincipalUser principalUser, @Valid @ModelAttribute ContentCreateRequest contentCreateRequest) {
        creatorWebtoonService.createContent(principalUser, contentCreateRequest);

        return ResponseEntity.ok().build();
    }

//    @GetMapping("/{webtoonId}")
//    public ResponseEntity<CreatorWebtoonResponse> getContentById(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long webtoonId) {
//
//        return ResponseEntity.ok(creatorWebtoonService.getContentById(principalUser, webtoonId));
//    }

    @GetMapping()
    public ResponseEntity<List<CreatorContentListResponse>> getMyContents(@AuthenticationPrincipal PrincipalUser principalUser) {

        return ResponseEntity.ok(creatorWebtoonService.getMyContents(principalUser));
    }

    @PatchMapping("/{webtoonId}")
    public ResponseEntity<Long> updateContent(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long webtoonId, @Valid @ModelAttribute ContentUpdateRequest contentUpdateRequest) {

        return ResponseEntity.ok(creatorWebtoonService.updateContent(principalUser, webtoonId, contentUpdateRequest));
    }

    @PostMapping("/{webtoonId}/delete-request")
    public ResponseEntity<Void> deleteRequestContent(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long webtoonId, @RequestBody ContentDeleteRequest contentDeleteRequest) {
        creatorWebtoonService.deleteRequestContent(principalUser, webtoonId, contentDeleteRequest);

        return ResponseEntity.ok().build();
    }
}
