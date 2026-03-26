package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.ReadingHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ContentResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Simple {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String author;
        private String contentType;
        private Double totalAverageRating;
        private Long totalRatingCount;

        public static Simple fromEntity(Content content) {
            return Simple.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .cover(content.getCover())
                    .author(content.getCreator().getPenName())
                    .contentType(content.getDtype())
                    .totalAverageRating(content.getTotalAverageRating())
                    .totalRatingCount(content.getTotalRatingCount())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String author;
        private String description;
        private String contentType;
        private Integer episodeCount;
        private Double totalAverageRating;
        private SerialDay serialDay;
        private SeriesStatus status;
        private Long viewCount;
        private List<KeywordResponse> keywords;

        public static Summary fromEntity(Content content) {
            return Summary.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .cover(content.getCover())
                    .author(content.getCreator().getPenName())
                    .description(content.getDescription())
                    .contentType(content.getDtype())
                    .episodeCount(content.getEpisodeCount())
                    .totalAverageRating(content.getTotalAverageRating())
                    .serialDay(content.getSerialDay())
                    .status(content.getStatus())
                    .viewCount(content.getViewCount())
                    .keywords(content.getContentKeywords().stream().map(KeywordResponse::fromEntity).toList())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String author;
        private String description;
        private String contentType;
        private Integer episodeCount;
        private Double totalAverageRating;
        private Long totalRatingCount;
        private SerialDay serialDay;
        private SeriesStatus status;
        private Long viewCount;
        private Boolean isInterested;
        private List<KeywordResponse> keywords;
        private List<EpisodeResponse.Summary> episodes;

        public static Detail fromEntity(Content content, List<EpisodeResponse.Summary> episodes, Boolean isInterested) {

            return Detail.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .author(content.getCreator().getPenName())
                    .cover(content.getCover())
                    .description(content.getDescription())
                    .contentType(content.getDtype())
                    .episodeCount(content.getEpisodeCount())
                    .totalAverageRating(content.getTotalAverageRating())
                    .totalRatingCount(content.getTotalRatingCount())
                    .serialDay(content.getSerialDay())
                    .status(content.getStatus())
                    .viewCount(content.getViewCount())
                    .keywords(content.getContentKeywords().stream().map(KeywordResponse::fromEntity).toList())
                    .episodes(episodes)
                    .isInterested(isInterested)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String author;
        private String description;
        private String contentType;
        private Integer episodeCount;
        private LocalDateTime episodeUpdatedAt;
        private Double totalAverageRating;
        private Long totalRatingCount;
        private List<KeywordResponse> keywords;

        public static Search fromEntity(Content content) {
            return Search.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .author(content.getCreator().getPenName())
                    .cover(content.getCover())
                    .description(content.getDescription())
                    .contentType(content.getDtype())
                    .episodeCount(content.getEpisodeCount())
                    .episodeUpdatedAt(content.getEpisodeUpdatedAt())
                    .totalAverageRating(content.getTotalAverageRating())
                    .totalRatingCount(content.getTotalRatingCount())
                    .keywords(content.getContentKeywords().stream().map(KeywordResponse::fromEntity).collect(Collectors.toList()))
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRead {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String author;
        private String contentType;
        private SerialDay serialDay;
        private SeriesStatus status;
        private LocalDateTime episodeUpdatedAt;
        private LocalDateTime lastReadAt;
        private Long lastReadEpisodeId;

        public static RecentRead fromEntity(ReadingHistory readingHistory) {
            return RecentRead.builder()
                    .contentId(readingHistory.getContent().getId())
                    .contentTitle(readingHistory.getContent().getTitle())
                    .cover(readingHistory.getContent().getCover())
                    .author(readingHistory.getContent().getCreator().getPenName())
                    .contentType(readingHistory.getContent().getDtype())
                    .serialDay(readingHistory.getContent().getSerialDay())
                    .status(readingHistory.getContent().getStatus())
                    .episodeUpdatedAt(readingHistory.getContent().getEpisodeUpdatedAt())
                    .lastReadAt(readingHistory.getLastReadAt())
                    .lastReadEpisodeId(readingHistory.getEpisodeId())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestContent {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String author;
        private String contentType;
        private SerialDay serialDay;
        private SeriesStatus status;
        private LocalDateTime episodeUpdatedAt;

        public static InterestContent fromEntity(Content content) {
            return InterestContent.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .cover(content.getCover())
                    .author(content.getCreator().getPenName())
                    .contentType(content.getDtype())
                    .serialDay(content.getSerialDay())
                    .status(content.getStatus())
                    .episodeUpdatedAt(content.getEpisodeUpdatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordContent {
        private String keyword;
        private PageResponse<ContentResponse.Simple> contents;

        public static KeywordContent fromEntity(String keyword, PageResponse<ContentResponse.Simple> contents) {
            return KeywordContent.builder()
                    .keyword(keyword)
                    .contents(contents)
                    .build();
        }
    }
}
