package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.dto.request.episode.WebtoonEpisodeUpdate;
import com.pageon.backend.dto.response.episode.EpisodeImage;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.entity.WebtoonImage;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.WebtoonImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebtoonImageService {

    private final WebtoonImageRepository webtoonImageRepository;
    private final CloudFrontSignerService cloudFrontSignerService;
    private final FileUploadService fileUploadService;

    public List<EpisodeImage> getWebtoonImages(List<WebtoonImage> webtoonImages) {
        List<EpisodeImage> images = new ArrayList<>();

        for (WebtoonImage webtoonImage : webtoonImages) {
            images.add(EpisodeImage.of(webtoonImage, cloudFrontSignerService.signUrl(webtoonImage.getImageUrl())));
        }

        return images;
    }
    @ExecutionTimer
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

    public void updateWebtoonImages(WebtoonEpisode episode, WebtoonEpisodeUpdate request, MultipartFile[] newFiles) {
        int newImagesSize = (newFiles == null) ? 0 : newFiles.length;
        validateSequence(request, newImagesSize);

        List<Long> keepImageIds = request.getExistingImages().stream()
                .map(WebtoonEpisodeUpdate.ExistingImage::getId)
                .toList();

        List<WebtoonImage> currentImages = episode.getImages();

        List<WebtoonImage> imagesToDelete = currentImages.stream()
                .filter(img -> !keepImageIds.contains(img.getId()))
                .toList();

        imagesToDelete.forEach(img -> {
            fileUploadService.deleteFile(img.getImageUrl());
            webtoonImageRepository.delete(img);
        });

        request.getExistingImages().forEach(existing -> {
            WebtoonImage image = webtoonImageRepository.findById(existing.getId()).orElseThrow(
                    () -> new CustomException(ErrorCode.IMAGE_NOT_FOUND)
            );

            image.updateSequence(existing.getSequence());
        });

        if (newFiles != null && newFiles.length > 0) {
            List<Integer> newImageSequences = request.getNewImageSequences();
            String s3Prefix = String.format("webtoons/%d/episode/%d", episode.getWebtoon().getId(), episode.getEpisodeNum());
            List<String> s3Urls = fileUploadService.uploadMultiple(newFiles, s3Prefix);

            for (int i = 0; i < s3Urls.size(); i++) {
                episode.getImages().add(WebtoonImage.builder()
                        .sequence(newImageSequences.get(i))
                        .imageUrl(s3Urls.get(i))
                        .webtoonEpisode(episode)
                        .build()
                );
            }
        }
    }

    public void deleteWebtoonImages(WebtoonEpisode episode) {
        List<WebtoonImage> images = episode.getImages();

        images.forEach(image -> {
            try {
                fileUploadService.deleteFile(image.getImageUrl());
            } catch (CustomException e) {
                log.error("S3 이미지 삭제 실패 - imageUrl: {}, error: {}", image.getImageUrl(), e.getMessage());
            }
        });

        webtoonImageRepository.deleteAll(episode.getImages());

    }

    private void validateSequence(WebtoonEpisodeUpdate request, int newImagesSize) {
        List<Integer> allSequences = new ArrayList<>();

        request.getExistingImages().stream()
                .map(WebtoonEpisodeUpdate.ExistingImage::getSequence)
                .forEach(allSequences::add);

        allSequences.addAll(request.getNewImageSequences());

        Set<Integer> uniqueSequences = new HashSet<>(allSequences);
        if (uniqueSequences.size() != allSequences.size()) {
            throw new CustomException(ErrorCode.DUPLICATE_SEQUENCE);
        }

        if (request.getNewImageSequences().size() != newImagesSize) {
            throw new CustomException(ErrorCode.INVALID_SEQUENCE);
        }
    }



}
