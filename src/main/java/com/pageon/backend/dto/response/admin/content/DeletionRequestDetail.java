package com.pageon.backend.dto.response.admin.content;

import com.pageon.backend.common.enums.DeleteReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionRequestDetail {

    private Long requestId;
    private DeleteReason deleteReason;
    private String reasonDetail;

}
