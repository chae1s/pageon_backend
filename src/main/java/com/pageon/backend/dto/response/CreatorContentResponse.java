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

        public static ContentList fromEntity(Content content) {
            return ContentList.builder()
                    .contentId(content.getId())
                    .contentTitle(content.getTitle())
                    .cover(content.getCover())
                    .contentType(content.getDtype())
                    .workStatus(content.getWorkStatus())
                    .seriesStatus(content.getStatus())
                    .serialDay(content.getSerialDay())
                    .episodeUpdatedAt(content.getEpisodeUpdatedAt())
                    .build();
        }

    }
}
