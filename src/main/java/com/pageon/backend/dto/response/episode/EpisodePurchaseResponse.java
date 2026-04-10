package com.pageon.backend.dto.response.episode;

import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.entity.EpisodePurchase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodePurchaseResponse {
    private PurchaseType purchaseType;
    private LocalDateTime expiredAt;

    public static EpisodePurchaseResponse of(EpisodePurchase episodePurchase) {
        return EpisodePurchaseResponse.builder()
                .purchaseType(episodePurchase.getPurchaseType())
                .expiredAt(episodePurchase.getExpiredAt())
                .build();
    }
}
