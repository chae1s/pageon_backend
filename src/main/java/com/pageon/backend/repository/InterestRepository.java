package com.pageon.backend.repository;

import com.pageon.backend.entity.Interest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Long> {
    Boolean existsByUser_IdAndContentId(Long userId, Long contentId);

    Optional<Interest> findByUser_IdAndContentId(Long userId, Long contentId);

    Page<Interest> findAllByUser_Id(Long userId, Pageable pageable);


    @Query(value = "SELECT DISTINCT i FROM Interest i " +
            "JOIN FETCH i.content c " +
            "JOIN FETCH c.creator " +
            "WHERE i.user.id = :userId",
            countQuery = "SELECT COUNT(DISTINCT i.id) FROM Interest i " +
                    "WHERE i.user.id = :userId"
    )
    Page<Interest> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTINCT i FROM Interest i " +
            "JOIN FETCH i.content c " +
            "JOIN FETCH c.creator " +
            "WHERE i.user.id = :userId AND TYPE(c) = Webnovel",
            countQuery = "SELECT COUNT(DISTINCT i.id) FROM Interest i " +
                    "JOIN i.content c " +
                    "WHERE i.user.id = :userId AND TYPE(c) = Webnovel "
    )
    Page<Interest> findWebnovelsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTINCT i FROM Interest i " +
            "JOIN FETCH i.content c " +
            "JOIN FETCH c.creator " +
            "WHERE i.user.id = :userId AND TYPE(c) = Webtoon ",
            countQuery = "SELECT COUNT(DISTINCT i.id) FROM Interest i " +
                    "JOIN i.content c " +
                    "WHERE i.user.id = :userId AND TYPE(c) = Webtoon "
    )
    Page<Interest> findWebtoonsByUserId(@Param("userId") Long userId, Pageable pageable);


}
