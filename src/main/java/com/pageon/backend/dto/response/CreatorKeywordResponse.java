package com.pageon.backend.dto.response;

import com.pageon.backend.entity.Keyword;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorKeywordResponse {

    private Long categoryId;
    private String name;

    public static CreatorKeywordResponse fromEntity(Keyword keyword) {
        CreatorKeywordResponse creatorKeywordResponse = new CreatorKeywordResponse();
        creatorKeywordResponse.setCategoryId(keyword.getCategory().getId());
        creatorKeywordResponse.setName(keyword.getName());

        return creatorKeywordResponse;
    }

}
