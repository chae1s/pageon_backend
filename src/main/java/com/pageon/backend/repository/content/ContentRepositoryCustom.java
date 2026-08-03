package com.pageon.backend.repository.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.dto.response.content.ContentSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ContentRepositoryCustom {

    Optional<ContentDetailResponse> findContentDetail(Long contentId);

    List<ContentDetailResponse> findContentDetails(SerialDay serialDay);

    Page<ContentSearchResponse> searchContents(String contentType, String query, Pageable pageable);
}
