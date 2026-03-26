package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.dto.request.ContentUpdateRequest;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "contents")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE")
public abstract class Content extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "DTYPE", insertable = false, updatable = false)
    private String dtype;

    private String title;
    @Column(length = 1000)
    private  String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Builder.Default
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL)
    private List<ContentKeyword> contentKeywords = new ArrayList<>();

    private String cover;
    // 연재 요일
    @Enumerated(EnumType.STRING)
    private SerialDay serialDay;

    // 연재, 완결, 휴재
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private SeriesStatus status = SeriesStatus.ONGOING;
    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Double totalAverageRating = 0.0;
    @Builder.Default
    private Long totalRatingCount = 0L;

    // 최신 에피소드 업데이트 날짜
    private LocalDateTime episodeUpdatedAt;
    // 에피소드 수
    @Builder.Default
    private Integer episodeCount = 0;


    public void updateCover(String s3Url) {
        this.cover = s3Url;
    }

    public void updateContentInfo(ContentUpdateRequest request) {
        if (request.getTitle() != null) this.title = request.getTitle();
        if (request.getDescription() != null)this.description = request.getDescription();
        if (request.getSerialDay() != null) this.serialDay = SerialDay.valueOf(request.getSerialDay());
    }

    public void updateKeywords(List<Keyword> keywords) {
        if (keywords != null) {

        }
    }

    public void deleteContent() {
        this.setDeletedAt(LocalDateTime.now());
    }

    public void updateStatus(String status) {
        if (status != null) this.status = SeriesStatus.valueOf(status);
    }

    public void addRating(Integer score) {
        double totalScore = this.totalAverageRating * this.totalRatingCount;
        this.totalRatingCount++;
        this.totalAverageRating = (totalScore + score) / this.totalRatingCount;
    }

    public void updateRating(Integer oldScore, Integer newScore) {
        if (this.totalRatingCount == 0) return;

        this.totalAverageRating = this.totalAverageRating + ((double) (newScore - oldScore) / this.totalRatingCount);
    }

    public void updateEpisode() {
        this.episodeUpdatedAt = LocalDateTime.now();
        this.episodeCount++;
    }

    public void updateViewCount() {
        this.viewCount++;
    }
}
