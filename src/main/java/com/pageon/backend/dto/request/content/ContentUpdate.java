package com.pageon.backend.dto.request.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContentUpdate {
    private String title;
    private String description;
    private String keywords;
    private SerialDay serialDay;
    private SeriesStatus seriesStatus;
}
