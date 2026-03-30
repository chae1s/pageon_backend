package com.pageon.backend.controller;

import com.pageon.backend.dto.request.content.ContentCreate;
import com.pageon.backend.dto.request.content.ContentDelete;
import com.pageon.backend.dto.request.content.ContentUpdate;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.creator.content.*;
import com.pageon.backend.dto.response.creator.deletion.DeletionList;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/creators/contents")
@RequiredArgsConstructor
public class CreatorContentController {

    private final CreatorContentService creatorContentService;

    @PostMapping
    public ResponseEntity<Long> createContent(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestPart("data") ContentCreate request,
            @RequestPart("coverImage")MultipartFile coverImage
    ) {

        Long contentId = creatorContentService.createContent(principalUser.getId(), request, coverImage);

        return ResponseEntity.ok(contentId);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ContentList>> getMyContents(
            @AuthenticationPrincipal PrincipalUser principalUser, @PageableDefault(size = 15) Pageable pageable,
            @RequestParam String seriesStatus, @RequestParam String sort
    ) {
        Page<ContentList> contents =
                creatorContentService.getMyContents(principalUser.getId(), pageable, seriesStatus, sort);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/{contentId}/simple")
    public ResponseEntity<ContentSimple> getContentById(
            @AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long contentId
    ) {
        ContentSimple content = creatorContentService.getContentById(principalUser.getId(), contentId);

        return ResponseEntity.ok(content);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDetail> getContent(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long contentId) {

        ContentDetail content = creatorContentService.getContent(principalUser.getId(), contentId);
        return ResponseEntity.ok(content);
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<Void> updateContent(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestPart("data") ContentUpdate request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) {

        creatorContentService.updateContent(principalUser.getId(), contentId, request, coverImage);

        return ResponseEntity.ok().build();
    }


    @PostMapping("/{contentId}/delete-requests")
    public ResponseEntity<Void> requestContentDeletion(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestBody ContentDelete request
    ) {
        creatorContentService.requestContentDeletion(principalUser.getId(), contentId, request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/delete-requests")
    public ResponseEntity<PageResponse<DeletionList>> getMyDeletionRequests(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PageableDefault(sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DeletionList> contentDeletes = creatorContentService.getMyDeletionRequests(principalUser.getId(), pageable);

        return ResponseEntity.ok(new PageResponse<>(contentDeletes));
    }

    @DeleteMapping("/delete-requests/{deleteId}")
    public ResponseEntity<Void> cancelDeletionRequest(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long deleteId
    ) {
        creatorContentService.cancelDeletionRequest(principalUser.getId(), deleteId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ContentStats> getContentStats(@AuthenticationPrincipal PrincipalUser principalUser) {

        ContentStats contentStats = creatorContentService.getContentStats(principalUser.getId());

        return ResponseEntity.ok(contentStats);
    }

    @GetMapping("/{contentId}/dashboard")
    public ResponseEntity<ContentDashboard> getContentDashboard(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId
    ) {
        ContentDashboard contentDashboard = creatorContentService.getContentDashboard(principalUser.getId(), contentId);

        return ResponseEntity.ok(contentDashboard);
    }

}
