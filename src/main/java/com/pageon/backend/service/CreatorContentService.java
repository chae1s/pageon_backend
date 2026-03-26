package com.pageon.backend.service;

import com.pageon.backend.dto.request.ContentCreateRequest;
import com.pageon.backend.dto.request.ContentDeleteRequest;
import com.pageon.backend.dto.request.ContentUpdateRequest;
import com.pageon.backend.dto.response.CreatorContentListResponse;
import com.pageon.backend.security.PrincipalUser;

import java.util.List;

interface CreatorContentService {

    void createContent(PrincipalUser principalUser, ContentCreateRequest contentCreateRequest);

    // 내가 작성한 content 리스트를 가져오는 메소드
    List<CreatorContentListResponse> getMyContents(PrincipalUser principalUser);

    Long updateContent(PrincipalUser principalUser, Long contentId, ContentUpdateRequest contentUpdateRequest);

    void deleteRequestContent(PrincipalUser principalUser, Long contentId, ContentDeleteRequest contentDeleteRequest);
}
