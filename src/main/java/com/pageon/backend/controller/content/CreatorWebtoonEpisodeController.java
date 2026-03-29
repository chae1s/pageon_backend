package com.pageon.backend.controller.content;

import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeCreate;
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
    public ResponseEntity<Void> createWebnovelEpisode(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable Long contentId,
            @RequestPart("data") WebtoonEpisodeCreate request,
            @RequestPart("files")MultipartFile[] files

    ) {

        creatorEpisodeService.createWebtoonEpisode(principalUser.getId(), contentId, request, files);

        return ResponseEntity.ok().build();
    }

}
