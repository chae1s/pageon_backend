package com.pageon.backend.entity.base;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.entity.Content;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Getter
@SuperBuilder
@DynamicUpdate
@MappedSuperclass
@NoArgsConstructor
public abstract class EpisodeBase extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer episodeNum;
    private String episodeTitle;

    private Integer purchasePrice;

    public Integer getRentalPrice() {
        return null;
    }

    @Builder.Default
    @Setter(AccessLevel.PROTECTED)
    private Double averageRating = 0.0;

    @Builder.Default
    @Setter(AccessLevel.PROTECTED)
    private Long ratingCount = 0L;

    private LocalDate publishedAt;

    @Enumerated(EnumType.STRING)
    private EpisodeStatus episodeStatus;

    public abstract Content getParentContent();

    public void addRating(Integer score) {
        double totalScore = this.averageRating * this.ratingCount;
        this.ratingCount++;
        this.averageRating = (totalScore + score) / this.ratingCount;
    }

    public void updateRating(Integer oldScore, Integer newScore) {
        if (this.ratingCount == 0) return;

        this.averageRating = this.averageRating + ((double) (newScore - oldScore) / this.ratingCount);
    }

}
