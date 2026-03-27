package com.pageon.backend.service;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.dto.request.ContentRequest;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.CreatorContentResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.Webnovel;
import com.pageon.backend.entity.Webtoon;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.CreatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorContentService {

    private final CreatorRepository creatorRepository;
    private final KeywordService keywordService;
    private final FileUploadService fileUploadService;
    private final ContentRepository contentRepository;

    @Transactional
    public void createContent(Long userId, ContentRequest.Create request) {
        Creator creator = creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );
        log.info("publishedAt: {}", request.getWorkStatus());
        if (request.getPublishedAt().isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_PUBLISHED_AT);
        }

        Content content = null;
        if (request.getContentType().equals("webnovels")) {
            content = createWebnovel(creator, request);
        } else if (request.getContentType().equals("webtoons")) {
            content = createWebtoon(creator, request);
        } else {
            throw new CustomException(ErrorCode.INVALID_CONTENT_TYPE);
        }

        keywordService.registerContentKeyword(content, request.getKeywords());

        contentRepository.save(content);

        String s3Url = fileUploadService.upload(request.getCoverImage(), String.format("%s/%d", request.getContentType(), content.getId()));

        content.updateCover(s3Url);

    }

    private Webnovel createWebnovel(Creator creator, ContentRequest.Create request) {

        return Webnovel.builder()
                .title(request.getTitle())
                .creator(creator)
                .description(request.getDescription())
                .serialDay(findSerialDay(request.getPublishedAt()))
                .publishedAt(request.getPublishedAt())
                .workStatus(request.getWorkStatus())
                .build();

    }

    private Webtoon createWebtoon(Creator creator, ContentRequest.Create request) {

        return Webtoon.builder()
                .title(request.getTitle())
                .creator(creator)
                .description(request.getDescription())
                .serialDay(findSerialDay(request.getPublishedAt()))
                .publishedAt(request.getPublishedAt())
                .workStatus(request.getWorkStatus())
                .build();

    }

    private SerialDay findSerialDay(LocalDate publishedAt) {
        int dayOfWeekValue = publishedAt.getDayOfWeek().getValue();

        int targetIndex = dayOfWeekValue % 7;

        return SerialDay.values()[targetIndex];
    }

    public Page<CreatorContentResponse.ContentList> getMyContents(Long userId, Pageable pageable, String seriesStatus, String sort) {
        Creator creator = creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );

        Pageable creatorContentPageable = PageableUtil.creatorContentPageable(pageable, sort);
        Page<Content> contents = contentRepository.findByCreator_IdAndStatus(creator.getId(), SeriesStatus.valueOf(seriesStatus), creatorContentPageable);

        return contents.map(CreatorContentResponse.ContentList::fromEntity);

    }




}
