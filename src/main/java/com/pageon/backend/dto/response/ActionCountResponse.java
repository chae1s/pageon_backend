package com.pageon.backend.dto.response;

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
public class ActionCountResponse {
    private Long contentId;
    private ContentType contentType;
    private ActionType actionType;
    private Long totalCount;
}
