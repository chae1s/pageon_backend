package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

public class CreatorContentResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentList {
        private Long contentId;
        private String contentTitle;
        private String cover;
        private String contentType;
        private WorkStatus workStatus;
        private SeriesStatus seriesStatus;
        private LocalDateTime episodeUpdatedAt;
        private SerialDay serialDay;
        private List<String> keywords;

        public static ContentList fromEntity(Content content) {
            List<String> keywords = content.getContentKeywords().stream()
                    .map(ck -> ck.getKeyword().getName())
                    .toList();

            return ContentList.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .cover(content.getCover())
                    .contentType(content.getDtype())
                    .workStatus(content.getWorkStatus())
                    .seriesStatus(content.getStatus())
                    .serialDay(content.getSerialDay())
                    .episodeUpdatedAt(content.getEpisodeUpdatedAt())
                    .keywords(keywords)
                    .build();
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Simple {
        private Long contentId;
        private String contentTitle;
        private String contentType;
        private String cover;
        private SeriesStatus seriesStatus;
        private SerialDay serialDay;

        public static Simple fromEntity(Content content) {
            return Simple.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .contentType(content.getDtype())
                    .cover(content.getCover())
                    .seriesStatus(content.getStatus())
                    .serialDay(content.getSerialDay())
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
        private String description;
        private String keywords;
        private SerialDay serialDay;
        private String cover;

        public static Detail fromEntity(Content content, String keywords) {
            return Detail.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .description(content.getDescription())
                    .keywords(keywords)
                    .cover(content.getCover())
                    .serialDay(content.getSerialDay())
                    .build();
        }
    }

}
