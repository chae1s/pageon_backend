package com.pageon.backend.entity;

import com.pageon.backend.common.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "content_rankings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContentRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_Id")
    private Content content;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;
    private Integer rankNo;
    private Long totalScore;

    private LocalDateTime rankingHour;
}
