package com.pageon.backend.dto.request.content;

import com.pageon.backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContentInfo {
    private Long contentId;
    private ContentType contentType;
    private Long episodeId;
}
