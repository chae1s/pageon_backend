package com.pageon.backend.controller.episode;

import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeUpdate;
import com.pageon.backend.dto.response.creator.episode.WebnovelEpisodeDetail;
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
    public ResponseEntity<Long> createWebnovelEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestBody WebnovelEpisodeCreate request
    ) {

        Long episodeId = creatorEpisodeService.createWebnovelEpisode(principalUser.getId(), contentId, request);

        return ResponseEntity.ok(episodeId);
    }

    @GetMapping("/{episodeId}")
    public ResponseEntity<WebnovelEpisodeDetail> getWebnovelEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @PathVariable Long episodeId
    ) {

        WebnovelEpisodeDetail episode = creatorEpisodeService.getWebnovelEpisodeDetail(principalUser.getId(), episodeId);

        return ResponseEntity.ok().body(episode);
    }

    @PatchMapping("/{episodeId}")
    public ResponseEntity<Long> updateWebnovelEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId, @PathVariable Long episodeId,
            @RequestBody WebnovelEpisodeUpdate request
    ) {

        Long result = creatorEpisodeService.updateWebnovelEpisode(principalUser.getId(), episodeId, request);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{episodeId}")
    public ResponseEntity<Void> deleteWebnovelEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId, @PathVariable Long episodeId
    ) {
        creatorEpisodeService.deleteWebnovelEpisode(principalUser.getId(), episodeId);

        return ResponseEntity.ok().build();
    }




}
