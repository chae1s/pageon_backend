package com.pageon.backend.repository.content;

import com.pageon.backend.dto.response.content.ContentDetailResponse;

import java.util.Optional;

public interface ContentRepositoryCustom {

    Optional<ContentDetailResponse> findContentDetail(Long contentId);
}
