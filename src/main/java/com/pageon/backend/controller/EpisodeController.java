package com.pageon.backend.controller;

import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.dto.request.EpisodeCommentRequest;
import com.pageon.backend.dto.request.EpisodeRatingRequest;
import com.pageon.backend.dto.response.*;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.EpisodeCommentService;
import com.pageon.backend.service.EpisodePurchaseService;
import com.pageon.backend.service.EpisodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/{contentType}/{contentId}/episodes")
public class EpisodeController {
    private final EpisodeService episodeService;
    private final EpisodeCommentService episodeCommentService;
    private final EpisodePurchaseService episodePurchaseService;


    @GetMapping("/{episodeId}")
    public ResponseEntity<Object> getEpisodeDetail(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId
    ) {
        Object episode = episodeService.getEpisodeDetail(principalUser.getId(), contentType, episodeId);

        return ResponseEntity.ok(episode);
    }

    @PostMapping("/{episodeId}/rating")
    public ResponseEntity<Void> rateEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @RequestBody EpisodeRatingRequest request
    ) {
        episodeService.rateEpisode(principalUser.getId(), contentType, episodeId, request);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{episodeId}/rating")
    public ResponseEntity<Void> updateRating(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @RequestBody EpisodeRatingRequest request
    ) {
        episodeService.updateEpisodeRating(principalUser.getId(), contentType, episodeId, request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{episodeId}/comments")
    public ResponseEntity<Void> createComment(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @RequestBody EpisodeCommentRequest request

    ) {
        episodeCommentService.createComment(principalUser.getId(), contentType, episodeId, request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{episodeId}/comments")
    public ResponseEntity<PageResponse<CommentResponse.Summary>> getComments(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @PageableDefault(size = 15) Pageable pageable,
            @RequestParam("sort") String sort
    ) {
        Page<CommentResponse.Summary> comments = episodeCommentService.getComments(principalUser.getId(), contentType, episodeId, pageable, sort);

        return ResponseEntity.ok(new PageResponse<>(comments));
    }

    @PatchMapping("/{episodeId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId,
            @RequestBody EpisodeCommentRequest request
    ) {
        episodeCommentService.updateComment(principalUser.getId(), contentType, commentId, request);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{episodeId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId
    ) {
        episodeCommentService.deleteComment(principalUser.getId(), contentType, commentId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{episodeId}/comments/{commentId}/likes")
    public ResponseEntity<Void> likeComment(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @PathVariable Long commentId
    ) {
        episodeCommentService.toggleCommentLike(principalUser.getId(), contentType, commentId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{episodeId}/subscribe")
    public ResponseEntity<Void> createPurchaseHistory(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId,
            @RequestParam PurchaseType purchaseType
    ) {
        episodePurchaseService.createPurchaseHistory(principalUser.getId(), contentType, episodeId, purchaseType);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{episodeId}/subscribe")
    public ResponseEntity<Boolean> checkPurchaseHistory(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId,
            @PathVariable Long episodeId
    ) {
        Boolean check = episodePurchaseService.checkPurchaseHistory(principalUser.getId(), contentId, episodeId);

        return ResponseEntity.ok(check);
    }

}
