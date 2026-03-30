package com.pageon.backend.controller.episode;

import com.pageon.backend.dto.request.episode.WebtoonEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeUpdate;
import com.pageon.backend.dto.response.creator.episode.WebnovelEpisodeDetail;
import com.pageon.backend.dto.response.creator.episode.WebtoonEpisodeDetail;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.CreatorEpisodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/creators/webtoons/{contentId}/episodes")
@RequiredArgsConstructor
public class CreatorWebtoonEpisodeController {

    private final CreatorEpisodeService creatorEpisodeService;

    @PostMapping
    public ResponseEntity<Long> createWebtoonEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestPart("data") WebtoonEpisodeCreate request,
            @RequestPart("files")MultipartFile[] files

    ) {

        Long episodeId = creatorEpisodeService.createWebtoonEpisode(principalUser.getId(), contentId, request, files);

        return ResponseEntity.ok(episodeId);
    }

    @GetMapping("/{episodeId}")
    public ResponseEntity<WebtoonEpisodeDetail> getWebtoonEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @PathVariable Long episodeId
    ) {

        WebtoonEpisodeDetail episode = creatorEpisodeService.getWebtoonEpisodeDetail(principalUser.getId(), episodeId);

        return ResponseEntity.ok().body(episode);
    }

    @PatchMapping("/{episodeId}")
    public ResponseEntity<Long> updateWebtoonEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId, @PathVariable Long episodeId,
            @RequestPart("data")WebtoonEpisodeUpdate request, @RequestPart("newFiles") MultipartFile[] newImages
    ) {
        Long result = creatorEpisodeService.updateWebtoonEpisode(principalUser.getId(), episodeId, request, newImages);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("{/episodeId}")
    public ResponseEntity<Void> deleteWebtoonEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId, @PathVariable Long episodeId
    ) {
        creatorEpisodeService.deleteWebtoonEpisode(principalUser.getId(), episodeId);

        return ResponseEntity.ok().build();
    }

}
