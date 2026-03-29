package com.pageon.backend.service;

import com.pageon.backend.common.enums.DeleteStatus;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.dto.request.content.ContentCreate;
import com.pageon.backend.dto.request.content.ContentDelete;
import com.pageon.backend.dto.request.content.ContentUpdate;
import com.pageon.backend.dto.response.creator.content.ContentDetail;
import com.pageon.backend.dto.response.creator.content.ContentList;
import com.pageon.backend.dto.response.creator.content.ContentSimple;
import com.pageon.backend.dto.response.creator.deletion.DeletionList;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentDeletionRequestRepository;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorContentService {

    private final CreatorRepository creatorRepository;
    private final KeywordService keywordService;
    private final FileUploadService fileUploadService;
    private final ContentRepository contentRepository;
    private final ContentDeletionRequestRepository contentDeletionRequestRepository;
    private final List<EpisodeProvider> providers;

    @Transactional
    public void createContent(Long userId, ContentCreate request, MultipartFile coverImage) {
        if (coverImage == null) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }

        Creator creator = getCreator(userId);

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

        contentRepository.save(content);

        keywordService.registerContentKeyword(content, request.getKeywords());

        String s3Url = fileUploadService.upload(coverImage, String.format("%s/%d", request.getContentType(), content.getId()));

        content.updateCover(s3Url);

    }

    private Webnovel createWebnovel(Creator creator, ContentCreate request) {

        return Webnovel.builder()
                .title(request.getTitle())
                .creator(creator)
                .description(request.getDescription())
                .serialDay(findSerialDay(request.getPublishedAt()))
                .publishedAt(request.getPublishedAt())
                .workStatus(request.getWorkStatus())
                .build();

    }

    private Webtoon createWebtoon(Creator creator, ContentCreate request) {

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

    public Page<ContentList> getMyContents(Long userId, Pageable pageable, String seriesStatus, String sort) {
        Creator creator = getCreator(userId);

        Pageable creatorContentPageable = PageableUtil.creatorContentPageable(pageable, sort);
        Page<Content> contents = contentRepository.findByCreator_IdAndStatusAndDeletedAtIsNull(creator.getId(), SeriesStatus.valueOf(seriesStatus), creatorContentPageable);

        return contents.map(ContentList::fromEntity);

    }


    public ContentSimple getContentById(Long userId, Long contentId) {
        Creator creator = getCreator(userId);

        ContentSimple content = contentRepository.findSimpleDtoByContentIdAndCreatorId(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        String contentType = (content.getContentType().equals("WEBNOVEL")) ? "webnovels" : "webtoons";

        EpisodeProvider episodeProvider = getProvider(contentType);
        int nextEpisodeNum = episodeProvider.getMaxEpisodeNum(contentId).orElse(0) + 1;

        content.setNextEpisodeNum(nextEpisodeNum);

        return content;
    }

    public ContentDetail getContent(Long userId, Long contentId) {
        Creator creator = getCreator(userId);

        Content content = contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        return ContentDetail.fromEntity(content, joinKeyword(content.getContentKeywords()));
    }

    private String joinKeyword(List<ContentKeyword> contentKeywords) {
        List<String> keywords = new ArrayList<>();
        for (ContentKeyword contentKeyword : contentKeywords) {
            keywords.add(contentKeyword.getKeyword().getName());
        }

        return String.join(",", keywords);
    }


    @Transactional
    public void updateContent(Long userId, Long contentId, ContentUpdate request, MultipartFile coverImage) {
        Creator creator = getCreator(userId);

        Content content = contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        content.updateContent(request);
        keywordService.updateContentKeyword(content, request.getKeywords());

        if (coverImage != null) {
            fileUploadService.deleteFile(content.getCover());

            String contentType = (content.getDtype().equals("WEBNOVEL")) ? "webnovels" : "webtoons";
            String newS3Url = fileUploadService.upload(coverImage, String.format("%s/%d", contentType, content.getId()));
            content.updateCover(newS3Url);
        }

    }


    @Transactional
    public void requestContentDeletion(Long userId, Long contentId, ContentDelete request) {
        Creator creator = getCreator(userId);

        Content content = contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        ContentDeletionRequest deletionRequest = ContentDeletionRequest.builder()
                .content(content)
                .creator(creator)
                .deleteReason(request.getDeleteReason())
                .reasonDetail(request.getReasonDetail())
                .deleteStatus(DeleteStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        content.deletionRequest();

        contentDeletionRequestRepository.save(deletionRequest);
    }

    public Page<DeletionList> getMyDeletionRequests(Long userId, Pageable pageable) {
        Creator creator = getCreator(userId);

        Page<ContentDeletionRequest> deletionRequests = contentDeletionRequestRepository.findByCreatorId(creator.getId(), pageable);

        return deletionRequests.map(DeletionList::fromEntity);
    }

    @Transactional
    public void cancelDeletionRequest(Long userId, Long deleteId) {
        Creator creator = getCreator(userId);

        ContentDeletionRequest deletionRequest = contentDeletionRequestRepository.findByIdAndCreator_Id(deleteId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.DELETION_REQUEST_NOT_FOUND)
        );

        if (deletionRequest.getDeleteStatus() != DeleteStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_CANCEL_DELETE_REQUEST);
        }

        deletionRequest.getContent().cancelDeletion();

        deletionRequest.cancelDeletion();

    }

    private Creator getCreator(Long userId) {
        return creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );
    }

    private EpisodeProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
