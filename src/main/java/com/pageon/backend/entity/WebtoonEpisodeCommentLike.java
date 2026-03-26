package com.pageon.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "webtoon_episode_comment_likes",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "UK_comment_like_user_comment", columnNames = {"user_id", "webtoon_episode_comment_id"}
            )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WebtoonEpisodeCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webtoon_episode_comment_id")
    private WebtoonEpisodeComment webtoonEpisodeComment;

}
