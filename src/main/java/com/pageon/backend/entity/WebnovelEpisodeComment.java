package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.entity.base.EpisodeCommentBase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@Table(name = "webnovel_episode_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebnovelEpisodeComment extends EpisodeCommentBase {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webnovel_episode_id")
    private WebnovelEpisode webnovelEpisode;

    @Builder.Default
    @OneToMany(mappedBy = "webnovelEpisodeComment", cascade = CascadeType.ALL,  orphanRemoval = true)
    private List<WebnovelEpisodeCommentLike> commentLikes = new ArrayList<>();


    @Override
    public EpisodeBase getParentEpisode() {
        return this.webnovelEpisode;
    }
}
