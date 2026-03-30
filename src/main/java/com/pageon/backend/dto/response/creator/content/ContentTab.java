package com.pageon.backend.dto.response.creator.content;

import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTab {
    private Long contentId;
    private String contentTitle;
    private String cover;
    private SeriesStatus seriesStatus;
    private WorkStatus workStatus;
    private String contentType;
}
