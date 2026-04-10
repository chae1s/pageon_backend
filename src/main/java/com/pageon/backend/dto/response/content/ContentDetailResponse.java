package com.pageon.backend.dto.response.content;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
public class ContentDetailResponse {
    private Long contentId;
    private ContentType contentType;
    private String contentTitle;
    private String cover;
    private String author;
    private String description;
    private SerialDay serialDay;
    private SeriesStatus seriesStatus;
    private Double totalAverageRating;
    private Long totalRatingCount;
    private Long viewCount;

    @Setter
    private List<KeywordResponse> keywords;
    @Setter
    private Boolean isInterested = false;

    @QueryProjection
    public ContentDetailResponse(
            Long contentId, String contentType, String contentTitle, String cover, String author, String description, SerialDay serialDay, SeriesStatus seriesStatus,
            Double totalAverageRating, Long totalRatingCount, Long viewCount
    ) {
        this.contentId = contentId;
        this.contentType = ContentType.valueOf(contentType);
        this.contentTitle = contentTitle;
        this.cover = cover;
        this.author = author;
        this.description = description;
        this.serialDay = serialDay;
        this.seriesStatus = seriesStatus;
        this.totalAverageRating = totalAverageRating;
        this.totalRatingCount = totalRatingCount;
        this.viewCount = viewCount;
    }


}
