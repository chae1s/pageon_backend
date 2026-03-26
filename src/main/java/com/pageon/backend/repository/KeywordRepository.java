package com.pageon.backend.repository;

import com.pageon.backend.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByName(String name);

    @Query("SELECT k FROM Keyword k " +
            "WHERE k.startDate <= :currentDate AND k.endDate >= :currentDate")
    Optional<Keyword> findValidKeyword(LocalDate currentDate);

}
