package com.pageon.backend.dto.response.content;

import com.pageon.backend.common.enums.ContentType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class ContentSearchResponse {
    private Long contentId;
    private String contentTitle;
    private String cover;
    private String author;
    private String description;
    private ContentType contentType;
    private Integer episodeCount;
    private LocalDateTime episodeUpdatedAt;
    private Double totalAverageRating;
    private Long totalRatingCount;

    @Setter
    private List<KeywordResponse> keywords;

    @QueryProjection
    public ContentSearchResponse(
            Long contentId, String contentTitle, String cover, String author, String description,
            String contentType, Integer episodeCount, LocalDateTime episodeUpdatedAt,
            Double totalAverageRating, Long totalRatingCount
    ) {
        this.contentId = contentId;
        this.contentTitle = contentTitle;
        this.cover = cover;
        this.author = author;
        this.description = description;
        this.contentType = ContentType.valueOf(contentType);
        this.episodeCount = episodeCount;
        this.episodeUpdatedAt = episodeUpdatedAt;
        this.totalAverageRating = totalAverageRating;
        this.totalRatingCount = totalRatingCount;
    }
}
