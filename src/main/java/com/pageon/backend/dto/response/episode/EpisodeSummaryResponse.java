package com.pageon.backend.dto.response.episode;

import com.pageon.backend.dto.mapping.EpisodeSummaryMapping;
import com.querydsl.core.annotations.QueryProjection;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeSummaryResponse {
    private Long episodeId;
    private Integer episodeNum;
    private String episodeTitle;
    private LocalDate publishedAt;
    private Integer purchasePrice;
    private Integer rentalPrice;

    @Setter
    private EpisodePurchaseResponse episodePurchase;

    @QueryProjection
    public EpisodeSummaryResponse(Long episodeId, Integer episodeNum, String episodeTitle, LocalDate publishedAt, Integer purchasePrice, Integer rentalPrice) {
        this.episodeId = episodeId;
        this.episodeNum = episodeNum;
        this.episodeTitle = episodeTitle;
        this.publishedAt = publishedAt;
        this.purchasePrice = purchasePrice;
        this.rentalPrice = rentalPrice;
    }

    public static EpisodeSummaryResponse of(EpisodeSummaryMapping mapping) {
        return EpisodeSummaryResponse.builder()
                .episodeId(mapping.getEpisodeId())
                .episodeNum(mapping.getEpisodeNum())
                .episodeTitle(mapping.getEpisodeTitle())
                .publishedAt(mapping.getPublishedAt())
                .purchasePrice(mapping.getPurchasePrice())
                .rentalPrice(mapping.getRentalPrice())
                .build();
    }
}
