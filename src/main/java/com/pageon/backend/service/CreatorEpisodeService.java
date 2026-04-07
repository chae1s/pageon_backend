package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeUpdate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeUpdate;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.creator.episode.*;
import com.pageon.backend.dto.response.episode.EpisodeImage;
import com.pageon.backend.entity.*;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.service.kafka.NotificationEventProducer;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorEpisodeService {

    private final CreatorRepository creatorRepository;
    private final WebnovelRepository webnovelRepository;
    private final WebtoonRepository webtoonRepository;
    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;
    private final WebtoonImageService webtoonImageService;
    private final ContentRepository contentRepository;
    private final List<EpisodeProvider> providers;


    @Transactional
    public Long createWebnovelEpisode(Long userId, Long contentId, WebnovelEpisodeCreate request) {
        Creator creator = getCreator(userId);

        Webnovel webnovel = webnovelRepository.findByIdAndCreator_IdAndDeletedAtIsNull(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        int nextNum = webnovelEpisodeRepository.findMaxEpisodeNumByContentId(contentId).orElse(0) + 1;

        WebnovelEpisode episode = WebnovelEpisode.builder()
                .webnovel(webnovel)
                .episodeNum(nextNum)
                .episodeTitle(request.getTitle())
                .publishedAt(request.getPublishedAt())
                .episodeStatus(EpisodeStatus.SCHEDULED)
                .content(request.getContent())
                .purchasePrice(100)
                .build();

        webnovelEpisodeRepository.save(episode);
        webnovel.updateEpisode(request.getPublishedAt());
        return episode.getId();
    }

    @Transactional
    @ExecutionTimer
    public Long createWebtoonEpisode(Long userId, Long contentId, WebtoonEpisodeCreate request, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new CustomException(ErrorCode.FILE_EMPTY);
        }

        Creator creator = getCreator(userId);

        Webtoon webtoon = webtoonRepository.findByIdAndCreator_IdAndDeletedAtIsNull(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        int nextNum = webtoonEpisodeRepository.findMaxEpisodeNumByContentId(contentId).orElse(0) + 1;

        WebtoonEpisode episode = WebtoonEpisode.builder()
                .webtoon(webtoon)
                .episodeNum(nextNum)
                .episodeTitle(request.getTitle())
                .publishedAt(request.getPublishedAt())
                .episodeStatus(EpisodeStatus.SCHEDULED)
                .purchasePrice(500)
                .rentalPrice(300)
                .build();

        webtoonEpisodeRepository.save(episode);

        webtoonImageService.registerWebtoonImage(contentId, episode, files);
        webtoon.updateEpisode(request.getPublishedAt());
        return episode.getId();
    }

    public EpisodeDashboard getEpisodeDashboard(Long userId, Long contentId, Pageable pageable, String status, String sort) {
        Creator creator = getCreator(userId);
        Content content = contentRepository.findByIdAndCreator_IdAndDeletedAtIsNull(contentId, creator.getId()).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        String contentType = (content.getDtype().equals("WEBNOVEL")) ? "webnovels" : "webtoons";
        EpisodeProvider provider = getProvider(contentType);

        Pageable dashboardPageable = PageableUtil.dashboardPageable(pageable, sort);

        Page<EpisodeList> episodes;
        if (status.equals("ALL")) {
            episodes = provider.getAllEpisodesByContent(contentId, dashboardPageable);
        } else {
            episodes = provider.getEpisodesByEpisodeStatus(contentId, EpisodeStatus.valueOf(status), dashboardPageable);
        }

        return EpisodeDashboard.builder()
                .contentTitle(content.getTitle())
                .contentType(contentType)
                .episodeStats(getEpisodeStats(provider, content))
                .episodes(new PageResponse<>(episodes))
                .build();

    }

    private EpisodeStats getEpisodeStats(EpisodeProvider provider, Content content) {

        Map<EpisodeStatus, Long> statusMap = provider.getGroupByStats(content.getId())
                .stream()
                .collect(Collectors.toMap(
                        row -> (EpisodeStatus) row[0],
                        row -> (Long) row[1]
                ));

        return new EpisodeStats(content.getEpisodeCount(), statusMap);
    }

    public WebnovelEpisodeDetail getWebnovelEpisodeDetail(Long userId, Long episodeId) {
        Creator creator = getCreator(userId);

        WebnovelEpisode episode = webnovelEpisodeRepository.findByIdAndDeletedAtIsNull(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (!episode.getWebnovel().getCreator().getId().equals(creator.getId())) {
            throw new CustomException(ErrorCode.NOT_CONTENT_OWNER);
        }

        return WebnovelEpisodeDetail.of(episode);
    }

    public WebtoonEpisodeDetail getWebtoonEpisodeDetail(Long userId, Long episodeId) {
        Creator creator = getCreator(userId);

        WebtoonEpisode episode = webtoonEpisodeRepository.findByIdAndDeletedAtIsNull(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (!episode.getWebtoon().getCreator().getId().equals(creator.getId())) {
            throw new CustomException(ErrorCode.NOT_CONTENT_OWNER);
        }

        List<EpisodeImage> images = webtoonImageService.getWebtoonImages(episode.getImages());

        return WebtoonEpisodeDetail.of(episode, images);
    }

    @Transactional
    public Long updateWebnovelEpisode(Long userId, Long episodeId, WebnovelEpisodeUpdate request) {
        Creator creator = getCreator(userId);
        WebnovelEpisode episode = webnovelEpisodeRepository.findById(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (!episode.getWebnovel().getCreator().getId().equals(creator.getId())) {
            throw new CustomException(ErrorCode.NOT_CONTENT_OWNER);
        }

        episode.updateEpisode(request.getTitle(), request.getPublishedAt());
        episode.updateContent(request.getContent());

        return episode.getId();
    }

    @Transactional
    public Long updateWebtoonEpisode(Long userId, Long episodeId, WebtoonEpisodeUpdate request, MultipartFile[] newFiles) {
        Creator creator = getCreator(userId);

        WebtoonEpisode episode = webtoonEpisodeRepository.findById(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (!episode.getWebtoon().getCreator().getId().equals(creator.getId())) {
            throw new CustomException(ErrorCode.NOT_CONTENT_OWNER);
        }

        episode.updateEpisode(request.getTitle(), request.getPublishedAt());

        webtoonImageService.updateWebtoonImages(episode, request, newFiles);

        return episode.getId();
    }

    @Transactional
    public void deleteWebnovelEpisode(Long userId, Long episodeId) {
        Creator creator = getCreator(userId);
        WebnovelEpisode episode = webnovelEpisodeRepository.findById(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (!episode.getWebnovel().getCreator().getId().equals(creator.getId())) {
            throw new CustomException(ErrorCode.NOT_CONTENT_OWNER);
        }

        episode.deleteEpisode();
    }

    @Transactional
    public void deleteWebtoonEpisode(Long userId, Long episodeId) {
        Creator creator = getCreator(userId);

        WebtoonEpisode episode = webtoonEpisodeRepository.findById(episodeId).orElseThrow(
                () -> new CustomException(ErrorCode.EPISODE_NOT_FOUND)
        );

        if (!episode.getWebtoon().getCreator().getId().equals(creator.getId())) {
            throw new CustomException(ErrorCode.NOT_CONTENT_OWNER);
        }

        webtoonImageService.deleteWebtoonImages(episode);
        episode.deleteEpisode();
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
