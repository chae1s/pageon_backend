package com.pageon.backend.entity.base;

import com.pageon.backend.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@SuperBuilder
@DynamicUpdate
@MappedSuperclass
@NoArgsConstructor
public abstract class EpisodeRatingBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Integer score;

    public void updateRating(Integer newScore) {
        this.score = newScore;
    }


}
