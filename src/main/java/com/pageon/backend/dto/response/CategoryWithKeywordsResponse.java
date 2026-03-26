package com.pageon.backend.dto.response;

import com.pageon.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWithKeywordsResponse {

    private Long id;
    private String name;
    private List<KeywordResponse> keywords;

    public static CategoryWithKeywordsResponse fromEntity(Category category) {

        return CategoryWithKeywordsResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .keywords(category.getKeywords() == null ? List.of() :
                        category.getKeywords().stream().map(KeywordResponse::fromEntity).toList())
                .build();
    }


}
