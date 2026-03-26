package com.pageon.backend.controller;

import com.pageon.backend.dto.response.CategoryWithKeywordsResponse;
import com.pageon.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final CategoryService categoryService;


    @GetMapping
    public ResponseEntity<List<CategoryWithKeywordsResponse>> getAllCategoriesWithKeywords() {

        return ResponseEntity.ok(categoryService.getAllCategoriesWithKeywords());
    }
}
