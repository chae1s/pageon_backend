package com.pageon.backend.entity.base;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.entity.User;
import com.pageon.backend.entity.WebnovelEpisode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@DynamicUpdate
@MappedSuperclass
@NoArgsConstructor
public abstract class EpisodeCommentBase extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String text;


    private Boolean isSpoiler;

    @Builder.Default
    private Long likeCount = 0L;

    public abstract EpisodeBase getParentEpisode();

    public void updateComment(String newText, Boolean isSpoiler) {
        this.text = newText;
        this.isSpoiler = isSpoiler;
    }

    public void deleteComment(LocalDateTime deleteTime) {
        this.setDeletedAt(deleteTime);
    }

    public void updateLikeCount() {
        this.likeCount = this.likeCount + 1;
    }

    public void deleteLikeCount() {
        this.likeCount = this.likeCount - 1;
    }

}
