package com.pageon.backend.entity.base;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.entity.Content;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Long viewCount;

    public void updateViewCount() {
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
        this.viewCount++;
    }

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

    public void updateEpisode(String episodeTitle, LocalDate publishedAt) {
        this.episodeTitle = episodeTitle;
        this.publishedAt = publishedAt;
    }

    public void deleteEpisode() {
        this.setDeletedAt(LocalDateTime.now());
    }

    public void publish() {
        this.episodeStatus = EpisodeStatus.PUBLISHED;
    }

}
