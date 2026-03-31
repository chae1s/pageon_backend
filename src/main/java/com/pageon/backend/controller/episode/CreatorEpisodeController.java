package com.pageon.backend.controller.episode;

import com.pageon.backend.dto.response.creator.episode.EpisodeDashboard;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorEpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/creators/contents")
@RequiredArgsConstructor
public class CreatorEpisodeController {

    private final CreatorEpisodeService creatorEpisodeService;

    @GetMapping("/{contentId}/episodes/dashboard")
    public ResponseEntity<EpisodeDashboard> getEpisodeDashboard(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            Pageable pageable,
            @RequestParam String status,
            @RequestParam String sort
    ) {

        EpisodeDashboard episodeDashboard =
                creatorEpisodeService.getEpisodeDashboard(principalUser.getId(), contentId, pageable, status, sort);

        return ResponseEntity.ok(episodeDashboard);
    }

}
