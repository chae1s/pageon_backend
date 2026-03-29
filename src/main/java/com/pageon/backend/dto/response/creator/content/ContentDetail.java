package com.pageon.backend.dto.response.creator.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDetail {
    private Long contentId;
    private String contentTitle;
    private String contentType;
    private SerialDay serialDay;
    private String cover;
    private String description;
    private String keywordLine;
    private SeriesStatus seriesStatus;

    public static ContentDetail fromEntity(Content content, String keywordLine) {
        List<String> keywords = content.getContentKeywords().stream()
                .map(ck -> ck.getKeyword().getName())
                .toList();

        return ContentDetail.builder()
                .contentId(content.getId())
                .contentTitle(content.getTitle())
                .contentType(content.getDtype())
                .serialDay(content.getSerialDay())
                .cover(content.getCover())
                .description(content.getDescription())
                .keywordLine(keywordLine)
                .seriesStatus(content.getStatus())
                .build();
    }
}
