package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.request.episode.WebnovelEpisodeCreate;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeCreate;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CreatorEpisodeService {

    private final CreatorRepository creatorRepository;
    private final WebnovelRepository webnovelRepository;
    private final WebtoonRepository webtoonRepository;
    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;
    private final WebtoonImageService webtoonImageService;


    @Transactional
    public void createWebnovelEpisode(Long userId, Long contentId, WebnovelEpisodeCreate request) {
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
                .purchasePrice(100)
                .build();

        webnovelEpisodeRepository.save(episode);
    }

    @Transactional
    public void createWebtoonEpisode(Long userId, Long contentId, WebtoonEpisodeCreate request, MultipartFile[] files) {
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

    }

    private Creator getCreator(Long userId) {
        return creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );
    }

}
