package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "content_action_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContentActionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contentId;
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;
    @Enumerated(EnumType.STRING)
    private ContentType contentType;
    @Builder.Default
    private Integer ratingScore = 0;
}
