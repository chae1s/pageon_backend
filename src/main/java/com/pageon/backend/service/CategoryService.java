package com.pageon.backend.service;

import com.pageon.backend.dto.response.CategoryWithKeywordsResponse;
import com.pageon.backend.entity.Category;
import com.pageon.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;


    public List<CategoryWithKeywordsResponse> getAllCategoriesWithKeywords() {
        List<Category> categories = categoryRepository.findAllWithKeywordsExcludingUncategorized();

        return categories.stream()
                .map(CategoryWithKeywordsResponse::fromEntity)
                .toList();
    }
}
