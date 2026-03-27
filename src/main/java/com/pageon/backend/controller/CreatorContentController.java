package com.pageon.backend.controller;

import com.pageon.backend.dto.request.ContentRequest;
import com.pageon.backend.dto.response.CreatorContentResponse;
import com.pageon.backend.dto.response.PageResponse;
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

    @GetMapping
    public ResponseEntity<PageResponse<CreatorContentResponse.ContentList>> getMyContents(
            @AuthenticationPrincipal PrincipalUser principalUser, @PageableDefault(size = 15) Pageable pageable,
            @RequestParam String seriesStatus, @RequestParam String sort
    ) {
        Page<CreatorContentResponse.ContentList> contents =
                creatorContentService.getMyContents(principalUser.getId(), pageable, seriesStatus, sort);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/simple")
    public ResponseEntity<PageResponse<CreatorContentResponse.Simple>> getSimpleContents(
            @AuthenticationPrincipal PrincipalUser principalUser, Pageable pageable, @RequestParam(required = false) String query
    ) {
        Page<CreatorContentResponse.Simple> contents = creatorContentService.getSimpleContents(principalUser.getId(), pageable, query);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<CreatorContentResponse.Detail> getContent(@AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long contentId) {

        CreatorContentResponse.Detail content = creatorContentService.getContent(principalUser.getId(), contentId);
        return ResponseEntity.ok(content);
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<Void> updateContent(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @ModelAttribute ContentRequest.Update request
    ) {

        creatorContentService.updateContent(principalUser.getId(), contentId, request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{contentId}/delete-info")
    public ResponseEntity<CreatorContentResponse.DeleteContent> getDeleteContent(
            @AuthenticationPrincipal PrincipalUser principalUser, @PathVariable Long contentId
    ) {
        CreatorContentResponse.DeleteContent content = creatorContentService.getDeleteContent(principalUser.getId(), contentId);

        return ResponseEntity.ok(content);
    }

    @PostMapping("/{contentId}/delete-requests")
    public ResponseEntity<Void> requestContentDeletion(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestBody ContentRequest.Delete request
    ) {
        creatorContentService.requestContentDeletion(principalUser.getId(), contentId, request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/delete-requests")
    public ResponseEntity<PageResponse<CreatorContentResponse.DeletionList>> getMyDeletionRequests(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PageableDefault(sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CreatorContentResponse.DeletionList> contentDeletes = creatorContentService.getMyDeletionRequests(principalUser.getId(), pageable);

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

}
