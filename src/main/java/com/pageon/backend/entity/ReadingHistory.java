package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "reading_histories", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_reading_history_user_content",
                columnNames = {"user_id", "content_id"}
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReadingHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    private Long episodeId;

    private LocalDateTime lastReadAt;

    public void updateEpisodeId(Long episodeId) {
        this.episodeId = episodeId;
        this.lastReadAt = LocalDateTime.now();
    }

}
