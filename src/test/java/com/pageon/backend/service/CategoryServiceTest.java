package com.pageon.backend.service;

import com.pageon.backend.dto.response.CategoryWithKeywordsResponse;
import com.pageon.backend.entity.Category;
import com.pageon.backend.entity.Keyword;
import com.pageon.backend.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@DisplayName("CategoryService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @InjectMocks
    private CategoryService categoryService;
    @Mock
    private CategoryRepository categoryRepository;


    @Test
    @DisplayName("uncategorized를 제외한 카테고리 목록을을 반환한다.")
    void getAllCategoriesWithKeywords_whenFetchingCategories_shouldExcludeUnassignedCategory() {
        // given

        Keyword genre1 = Keyword.builder().id(1L).name("SF").build();
        Keyword genre2 = Keyword.builder().id(2L).name("GAME").build();
        Keyword theme1 = Keyword.builder().id(3L).name("AI").build();

        Category category1 = Category.builder()
                .id(1L)
                .name("genre")
                .keywords(List.of(genre1, genre2))
                .build();
        Category category2 = Category.builder()
                .id(2L)
                .name("theme")
                .keywords(List.of(theme1))
                .build();

        when(categoryRepository.findAllWithKeywordsExcludingUncategorized())
                .thenReturn(List.of(category1, category2));
        
        //when
        List<CategoryWithKeywordsResponse> result = categoryService.getAllCategoriesWithKeywords();
        
        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(c -> c.getName().equals("uncategorized")));
        assertEquals(2, result.get(0).getKeywords().size());
        assertEquals("SF", result.get(0).getKeywords().get(0).getName());
    }

    @Test
    @DisplayName("카테고리가 없으면 빈 리스트를 반환한다.")
    void getAllCategoriesWithKeywords_whenNoCategoriesExist_shouldReturnEmptyList() {
        // given
        when(categoryRepository.findAllWithKeywordsExcludingUncategorized()).thenReturn(List.of());

        //when
        List<CategoryWithKeywordsResponse> result = categoryService.getAllCategoriesWithKeywords();

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    @DisplayName("키워드가 없는 카테고리도 반환한다.")
    void getAllCategoriesWithKeywords_whenCategoryHasNoKeywords_shouldReturnCategories() {
        // given
        Category category = Category.builder()
                .id(1L)
                .name("genre")
                .keywords(List.of())
                .build();

        when(categoryRepository.findAllWithKeywordsExcludingUncategorized()).thenReturn(List.of(category));

        //when
        List<CategoryWithKeywordsResponse> result = categoryService.getAllCategoriesWithKeywords();

        // then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getKeywords().isEmpty());

    }

}