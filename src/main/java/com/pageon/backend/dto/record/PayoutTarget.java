package com.pageon.backend.dto.record;

import com.pageon.backend.entity.Settlement;

public record PayoutTarget(
        Long creatorId,
        Settlement settlement
) {

}
