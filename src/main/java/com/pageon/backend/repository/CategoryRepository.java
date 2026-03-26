package com.pageon.backend.repository;

import com.pageon.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findById(Long id);

    @Query("SELECT DISTINCT c FROM Category  c LEFT JOIN FETCH c.keywords WHERE c.name <> 'uncategorized'")
    List<Category> findAllWithKeywordsExcludingUncategorized();
}
