package com.pageon.backend.dto.payload;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionLogEvent {

    private Long userId;
    private Long contentId;
    private ContentType contentType;
    private ActionType actionType;
    private Integer ratingScore;
}
