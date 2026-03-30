package com.pageon.backend.dto.response.episode;

import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.WebtoonImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeImage {
    private Long id;
    private Integer sequence;
    private String imageUrl;

    public static EpisodeImage of(WebtoonImage webtoonImage, String signUrl) {

        return EpisodeImage.builder()
                .id(webtoonImage.getId())
                .sequence(webtoonImage.getSequence())
                .imageUrl(signUrl)
                .build();
    }
}
