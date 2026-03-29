package com.pageon.backend.service;

import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.entity.WebtoonImage;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.repository.WebtoonImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebtoonImageService {

    private final WebtoonImageRepository webtoonImageRepository;
    private final CloudFrontSignerService cloudFrontSignerService;
    private final FileUploadService fileUploadService;

    public List<EpisodeResponse.EpisodeImage> getWebtoonImages(Long episodeId) {
        List<EpisodeResponse.EpisodeImage> images = new ArrayList<>();

        List<WebtoonImage> webtoonImages = webtoonImageRepository.findByWebtoonEpisodeIdOrderBySequenceAsc(episodeId);

        for (WebtoonImage webtoonImage : webtoonImages) {
            images.add(EpisodeResponse.EpisodeImage.fromEntity(webtoonImage, cloudFrontSignerService.signUrl(webtoonImage.getImageUrl())));
        }

        return images;
    }

    public void registerWebtoonImage(Long contentId, WebtoonEpisode webtoonEpisode, MultipartFile[] files) {

        String s3Prefix = String.format("webtoons/%d/episode/%d", contentId, webtoonEpisode.getEpisodeNum());
        List<String> s3Urls = fileUploadService.uploadMultiple(files, s3Prefix);


        for (int i = 0; i < s3Urls.size(); i++) {
            webtoonEpisode.getImages().add(WebtoonImage.builder()
                    .sequence(i + 1)
                    .imageUrl(s3Urls.get(i))
                    .webtoonEpisode(webtoonEpisode)
                    .build());

        }

    }



}
