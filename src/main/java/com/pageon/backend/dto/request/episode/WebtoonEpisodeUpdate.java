package com.pageon.backend.dto.request.episode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebtoonEpisodeUpdate {
    private String title;
    private LocalDate publishedAt;
    private List<ExistingImage> existingImages;
    private List<Integer> newImageSequences;

    @Getter
    @Builder
    public static class ExistingImage {
        private Long id;
        private Integer sequence;
    }
}
