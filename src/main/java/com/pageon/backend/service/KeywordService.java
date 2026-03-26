package com.pageon.backend.service;


import com.pageon.backend.entity.Category;
import com.pageon.backend.entity.Keyword;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CategoryRepository;
import com.pageon.backend.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final CategoryRepository categoryRepository;
    private static final Long UNCATEGORIZED_CATEGORY_ID = 6L;

    public List<Keyword> separateKeywords(String line) {
        if (line == null || line.isBlank()) {
            return new ArrayList<>();
        }

        String[] words = line.replaceAll("\\s", "").split(",");
        LinkedHashMap<String, Keyword> keywordMap = new LinkedHashMap<>();
        Category category = categoryRepository.findById(UNCATEGORIZED_CATEGORY_ID).orElseThrow(
                () -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND)
        );

        for (String word : words) {
            if (!keywordMap.containsKey(word)) {
                Keyword keyword = keywordRepository.findByName(word).orElseGet(
                        () -> keywordRepository.save(new Keyword(category, word))
                );

                keywordMap.put(word, keyword);
            }

        }
        return new ArrayList<>(keywordMap.values());
    }

}
