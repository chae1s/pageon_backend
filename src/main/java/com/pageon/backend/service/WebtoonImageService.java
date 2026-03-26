package com.pageon.backend.service;

import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.WebtoonImage;
import com.pageon.backend.repository.WebtoonImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebtoonImageService {

    private final WebtoonImageRepository webtoonImageRepository;
    private final CloudFrontSignerService cloudFrontSignerService;

    public List<EpisodeResponse.EpisodeImage> getWebtoonImages(Long episodeId) {
        List<EpisodeResponse.EpisodeImage> images = new ArrayList<>();

        List<WebtoonImage> webtoonImages = webtoonImageRepository.findByWebtoonEpisodeIdOrderBySequenceAsc(episodeId);

        for (WebtoonImage webtoonImage : webtoonImages) {
            images.add(EpisodeResponse.EpisodeImage.fromEntity(webtoonImage, cloudFrontSignerService.signUrl(webtoonImage.getImageUrl())));
        }

        return images;
    }



}
