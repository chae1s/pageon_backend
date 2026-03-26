package com.pageon.backend.entity;


import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@Table(name = "webtoon_episode_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebtoonEpisodeComment extends EpisodeCommentBase {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webtoon_episode_id")
    private WebtoonEpisode webtoonEpisode;

    @Builder.Default
    @OneToMany(mappedBy = "webtoonEpisodeComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WebtoonEpisodeCommentLike> commentLikes = new ArrayList<>();

    @Override
    public EpisodeBase getParentEpisode() {
        return this.webtoonEpisode;
    }
}
