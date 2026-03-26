package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.PurchaseType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "episode_purchases")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EpisodePurchase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long contentId;
    private Long episodeId;

    @Enumerated(EnumType.STRING)
    private PurchaseType purchaseType;

    private LocalDateTime expiredAt;


    public void extendRental(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public void upgradeToPurchase() {
        this.expiredAt = null;
        this.purchaseType = PurchaseType.OWN;
    }


}
