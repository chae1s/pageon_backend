package com.pageon.backend.dto.response;

import com.pageon.backend.entity.ContentKeyword;
import com.pageon.backend.entity.Keyword;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordResponse {

    private Long id;
    private String name;

    public static KeywordResponse fromEntity(Keyword keyword) {

        return KeywordResponse.builder()
                .id(keyword.getId())
                .name(keyword.getName())
                .build();
    }

    public static KeywordResponse fromEntity(ContentKeyword contentKeyword) {

        return KeywordResponse.builder()
                .id(contentKeyword.getKeyword().getId())
                .name(contentKeyword.getKeyword().getName())
                .build();
    }

}
