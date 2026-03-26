package com.pageon.backend.repository;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.entity.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory,Long> {

    Optional<ReadingHistory> findByUser_IdAndContent_Id(Long userId, Long contentId);

    @Query(value = "SELECT DISTiNCT r FROM ReadingHistory r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator " +
            "WHERE r.user.id = :userId",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM ReadingHistory r " +
                    "WHERE r.user.id = :userId"
    )
    Page<ReadingHistory> findAllReadingHistories(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTiNCT r FROM ReadingHistory r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator " +
            "WHERE r.user.id = :userId AND TYPE(c) = Webnovel",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM ReadingHistory r " +
                    "JOIN r.content c " +
                    "WHERE r.user.id = :userId AND TYPE(c) = Webnovel "
    )
    Page<ReadingHistory> findWebnovelReadingHistories(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTiNCT r FROM ReadingHistory r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator " +
            "WHERE r.user.id = :userId AND TYPE(c) = Webtoon",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM ReadingHistory r " +
                    "JOIN r.content c " +
                    "WHERE r.user.id = :userId AND TYPE(c) = Webtoon "
    )
    Page<ReadingHistory> findWebtoonReadingHistories(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT DISTINCT r FROM ReadingHistory r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator cr " +
            "WHERE r.user.id = :userId AND c.status = 'ONGOING' AND c.serialDay = :serialDay",
            countQuery = "SELECT COUNT(DISTINCT r.id) FROM ReadingHistory r " +
                    "JOIN FETCH r.content c " +
                    "WHERE r.user.id = :userId AND c.status = 'ONGOING' AND c.serialDay = :serialDay"
    )
    List<ReadingHistory> findWithContentByUserIdAndSerialDay(@Param("userId") Long userId, @Param("serialDay") SerialDay serialDay);
}
