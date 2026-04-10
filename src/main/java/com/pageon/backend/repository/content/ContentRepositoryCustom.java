package com.pageon.backend.repository.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.content.ContentDetailResponse;

import java.util.List;
import java.util.Optional;

public interface ContentRepositoryCustom {

    Optional<ContentDetailResponse> findContentDetail(Long contentId);

    List<ContentDetailResponse> findContentDetails(SerialDay serialDay);
}
