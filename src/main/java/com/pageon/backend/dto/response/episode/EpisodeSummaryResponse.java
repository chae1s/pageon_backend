package com.pageon.backend.dto.response.episode;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
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
}
