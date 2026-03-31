package com.pageon.backend.dto.request.content;

import com.pageon.backend.common.enums.DeleteReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContentDelete {
    private DeleteReason deleteReason;
    private String reasonDetail;
}
