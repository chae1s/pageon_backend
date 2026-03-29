package com.pageon.backend.controller.content;

import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorEpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators/webnovels/{contentId}/episodes")
@RequiredArgsConstructor
public class CreatorWebnovelEpisodeController {

    private final CreatorEpisodeService creatorEpisodeService;

    @PostMapping
    public ResponseEntity<Void> createWebnovelEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestBody WebnovelEpisodeCreate request
    ) {

        creatorEpisodeService.createWebnovelEpisode(principalUser.getId(), contentId, request);

        return ResponseEntity.ok().build();
    }



}
