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
@Table(name = "webnovel_episodes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebnovelEpisode extends EpisodeBase {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webnovel_id")
    private Webnovel webnovel;


    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @OneToMany(mappedBy = "webnovelEpisode", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<WebnovelEpisodeRating> webnovelEpisodeRatings = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "webnovelEpisode", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<WebnovelEpisodeComment> webnovelEpisodeComments = new ArrayList<>();

    @Override
    public Content getParentContent() {
        return this.webnovel;
    }

    @Override
    public void updateEpisode(String episodeTitle, LocalDate publishedAt) {
        super.updateEpisode(episodeTitle, publishedAt);
    }

    public void updateContent(String content) {
        this.content = content;
    }

}
