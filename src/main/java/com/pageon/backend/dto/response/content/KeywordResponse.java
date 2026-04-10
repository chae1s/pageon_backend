package com.pageon.backend.dto.response.content;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KeywordResponse {
    private Long keywordId;
    private String keyword;

    @QueryProjection
    public KeywordResponse(Long keywordId, String keyword) {
        this.keywordId = keywordId;
        this.keyword = keyword;
    }
}
