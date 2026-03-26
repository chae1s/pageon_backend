package com.pageon.backend.entity;

import com.pageon.backend.entity.base.EpisodeRatingBase;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@Table(name = "webnovel_episode_ratings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebnovelEpisodeRating extends EpisodeRatingBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webnovelEpisode_id")
    private WebnovelEpisode webnovelEpisode;



}
