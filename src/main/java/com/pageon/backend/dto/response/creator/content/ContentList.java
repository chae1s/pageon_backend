package com.pageon.backend.dto.response.creator.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentList {
    private Long contentId;
    private String contentTitle;
    private String contentType;
    private SerialDay serialDay;
    private String cover;
    private WorkStatus workStatus;
    private SeriesStatus seriesStatus;
    private LocalDateTime episodeUpdatedAt;
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
