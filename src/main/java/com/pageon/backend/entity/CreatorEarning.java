package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.EarningStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Builder
@ToString
@DynamicUpdate
@Table(name = "creator_earnings", indexes = {
        @Index(name = "idx_ce_status_created_creator", columnList = "earning_status, created_at, creator_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreatorEarning extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    private Integer point;

    private Long pointTransactionId;
    @Enumerated(EnumType.STRING)
    private EarningStatus earningStatus;

    public void complete() {
        this.earningStatus = EarningStatus.SETTLED;
    }



}
