package com.pageon.backend.dto.response;

import com.pageon.backend.entity.Keyword;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserKeywordResponse {
    private Long categoryId;
    private String name;

    public static UserKeywordResponse fromEntity(Keyword keyword) {
        UserKeywordResponse userKeywordResponse = new UserKeywordResponse();
        userKeywordResponse.setCategoryId(keyword.getCategory().getId());
        userKeywordResponse.setName(keyword.getName());

        return userKeywordResponse;
    }
}
