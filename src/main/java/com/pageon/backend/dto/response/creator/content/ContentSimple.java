package com.pageon.backend.dto.response.creator.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentSimple {
    private Long contentId;
    private String contentTitle;
    private String contentType;
    private SerialDay serialDay;
    @Setter
    private Integer nextEpisodeNum;

    public static ContentSimple fromEntity(Content content, Integer nextEpisodeNum) {
        return ContentSimple.builder()
                .contentId(content.getId())
                .contentTitle(content.getTitle())
                .contentType(content.getDtype())
                .serialDay(content.getSerialDay())
                .nextEpisodeNum(nextEpisodeNum)
                .build();
    }
}
