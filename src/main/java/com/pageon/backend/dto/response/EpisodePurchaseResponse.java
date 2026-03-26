package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.PurchaseType;
import com.pageon.backend.entity.EpisodePurchase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodePurchaseResponse {
    private Long id;
    private PurchaseType purchaseType;
    private LocalDateTime expiredAt;

    public static EpisodePurchaseResponse fromEntity(EpisodePurchase episodePurchase) {
        return EpisodePurchaseResponse.builder()
                .id(episodePurchase.getId())
                .purchaseType(episodePurchase.getPurchaseType())
                .expiredAt(episodePurchase.getExpiredAt())
                .build();
    }
}
