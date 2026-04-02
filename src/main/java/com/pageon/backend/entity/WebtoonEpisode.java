package com.pageon.backend.entity;

import com.pageon.backend.entity.base.EpisodeBase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@Table(name = "webtoon_episodes", indexes = {
        @Index(name = "idx_wte_published_status", columnList = "published_at, episode_status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebtoonEpisode extends EpisodeBase {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webtoon_id")
    private Webtoon webtoon;

    @Builder.Default
    @OneToMany(mappedBy = "webtoonEpisode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebtoonImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "webtoonEpisode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebtoonEpisodeRating> webtoonEpisodeRatings = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "webtoonEpisode", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebtoonEpisodeComment> webtoonEpisodeComments = new ArrayList<>();

    // 대여 금액
    private Integer rentalPrice;

    @Override
    public Integer getRentalPrice() {
        return rentalPrice;
    }

    public void addImage(WebtoonImage image) {
        this.images.add(image);
        image.addEpisode(this);
    }

    @Override
    public Content getParentContent() {
        return this.webtoon;
    }

    @Override
    public void updateEpisode(String episodeTitle, LocalDate publishedAt) {
        super.updateEpisode(episodeTitle, publishedAt);
    }
}
