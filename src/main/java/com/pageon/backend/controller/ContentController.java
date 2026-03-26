package com.pageon.backend.controller;

import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.ContentService;
import com.pageon.backend.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/{contentType}")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final RankingService rankingService;

    @GetMapping("/{contentId:\\d+}")
    public ResponseEntity<ContentResponse.Detail> getContentDetail(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId
    ) {

        ContentResponse.Detail detail = contentService.getContentDetail(principalUser, contentType, contentId);

        return ResponseEntity.ok().body(detail);
    }

    @GetMapping(params = "keyword")
    public ResponseEntity<PageResponse<ContentResponse.Search>> searchByKeyword(
            @PathVariable String contentType,
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 60) Pageable pageable,
            @RequestParam("sort") String sort
    ) {
        Page<ContentResponse.Search> contents = contentService.searchContentsByKeyword(contentType, keyword, pageable, sort);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping(params = "query")
    public ResponseEntity<PageResponse<ContentResponse.Search>> searchByQuery(
            @PathVariable String contentType,
            @RequestParam("query") String query,
            @PageableDefault(size = 60) Pageable pageable,
            @RequestParam("sort") String sort
    ) {
        Page<ContentResponse.Search> contents = contentService.searchContentsByTitleOrAuthor(contentType, query, pageable, sort);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/new")
    public ResponseEntity<List<ContentResponse.Simple>> getNewArrivals(
            @PathVariable String contentType
    ) {
        List<ContentResponse.Simple> contents = contentService.getNewArrivalList(contentType, LocalDate.now());

        return ResponseEntity.ok(contents);
    }

    @GetMapping("/new/more")
    public ResponseEntity<PageResponse<ContentResponse.Simple>> getNewArrivals(
            @PathVariable String contentType,
            Pageable pageable
    ) {
        Page<ContentResponse.Simple> contents = contentService.getNewArrivalPage(contentType, pageable);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/daily/{serialDay}")
    public ResponseEntity<List<ContentResponse.Simple>> getDailySchedules(
            @PathVariable String contentType,
            @PathVariable String serialDay
    ) {
        List<ContentResponse.Simple> contents = contentService.getDailyScheduleList(contentType, serialDay);

        return ResponseEntity.ok(contents);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<ContentResponse.Simple>> getBestCompleted(@PathVariable String contentType) {
        List<ContentResponse.Simple> contents = contentService.getBestCompletedList(contentType);

        return ResponseEntity.ok(contents);
    }

    @GetMapping("/completed/more")
    public ResponseEntity<PageResponse<ContentResponse.Simple>> getBestCompleted(
            @PathVariable String contentType,
            Pageable pageable
    ) {
        Page<ContentResponse.Simple> contents = contentService.getBestCompletedPage(contentType, pageable);

        return ResponseEntity.ok(new PageResponse<>(contents));
    }

    @GetMapping("/keyword")
    public ResponseEntity<ContentResponse.KeywordContent> getFeaturedByKeyword(@PathVariable String contentType) {
        ContentResponse.KeywordContent contents = contentService.getFeaturedKeywordContentsList(contentType);

        return ResponseEntity.ok(contents);
    }

    @GetMapping("/keyword/more")
    public ResponseEntity<ContentResponse.KeywordContent> getFeaturedByKeyword(
            @PathVariable String contentType,
            Pageable pageable
    ) {
        ContentResponse.KeywordContent contents = contentService.getFeaturedKeywordContentsPage(contentType, pageable);

        return ResponseEntity.ok(contents);
    }

    @PostMapping("/{contentId}/interests")
    public ResponseEntity<Void> handleInterest(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @PathVariable String contentType,
            @PathVariable Long contentId
    ) {
        contentService.toggleInterest(principalUser.getId(), contentId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/hourly-ranking")
    public ResponseEntity<List<ContentResponse.Simple>> getHourlyRank(@PathVariable String contentType) {
        log.info("Getting hourly ranking for content type {}", contentType);
        List<ContentResponse.Simple> contents = contentService.getBestCompletedList(contentType);
        return ResponseEntity.ok(contents);
    }
}
