package com.pageon.backend.dto.request;

import com.pageon.backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentInfoRequest {
    private Long contentId;
    private ContentType contentType;
    private Long episodeId;
}
