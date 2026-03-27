package com.pageon.backend.service;


import com.pageon.backend.entity.Category;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.ContentKeyword;
import com.pageon.backend.entity.Keyword;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CategoryRepository;
import com.pageon.backend.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final CategoryRepository categoryRepository;
    private static final Long UNCATEGORIZED_CATEGORY_ID = 6L;

    @Transactional
    public void registerContentKeyword(Content content, String keywordLine) {
        List<Keyword> keywords = separateKeywords(keywordLine);

        for (Keyword keyword : keywords) {
            ContentKeyword contentKeyword = ContentKeyword.builder()
                    .content(content)
                    .keyword(keyword)
                    .build();

            content.getContentKeywords().add(contentKeyword);
        }

    }

    private List<Keyword> separateKeywords(String line) {
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

    @Transactional
    public void updateContentKeyword(Content content, String keywordLine) {
        if (keywordLine == null || keywordLine.isBlank()) return;

        List<Keyword> keywords = separateKeywords(keywordLine);

        if (!checkChangeKeyword(content, keywords)) {
            return;
        }

        content.getContentKeywords().clear();
        for (Keyword keyword : keywords) {
            ContentKeyword contentKeyword = ContentKeyword.builder()
                    .content(content)
                    .keyword(keyword)
                    .build();

            content.getContentKeywords().add(contentKeyword);
        }

    }

    private boolean checkChangeKeyword(Content content, List<Keyword> keywords) {
        List<String> oldKeywordName = content.getContentKeywords().stream()
                .map(ck -> ck.getKeyword().getName())
                .toList();

        List<String> newKeywordName = keywords.stream()
                .map(Keyword::getName)
                .toList();

        return !new HashSet<>(oldKeywordName).equals(new HashSet<>(newKeywordName));
    }
}
