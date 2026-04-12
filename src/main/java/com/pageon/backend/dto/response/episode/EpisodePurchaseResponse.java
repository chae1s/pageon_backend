package com.pageon.backend.dto.response.episode;

import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.entity.EpisodePurchase;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
public class EpisodePurchaseResponse {
    private Long episodeId;
    private PurchaseType purchaseType;
    private LocalDateTime expiredAt;

    public static EpisodePurchaseResponse of(EpisodePurchase episodePurchase) {
        return EpisodePurchaseResponse.builder()
                .episodeId(episodePurchase.getEpisodeId())
                .purchaseType(episodePurchase.getPurchaseType())
                .expiredAt(episodePurchase.getExpiredAt())
                .build();
    }

    @QueryProjection
    public EpisodePurchaseResponse(Long episodeId, PurchaseType purchaseType, LocalDateTime expiredAt) {
        this.episodeId = episodeId;
        this.purchaseType = purchaseType;
        this.expiredAt = expiredAt;
    }
}
