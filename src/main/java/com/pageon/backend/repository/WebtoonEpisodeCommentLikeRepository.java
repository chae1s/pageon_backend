package com.pageon.backend.repository;

import com.pageon.backend.entity.WebtoonEpisodeCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebtoonEpisodeCommentLikeRepository extends JpaRepository<WebtoonEpisodeCommentLike, Long> {

    Boolean existsByUser_IdAndWebtoonEpisodeComment_Id(Long userId, Long commentId);

    Optional<WebtoonEpisodeCommentLike> findByUser_IdAndWebtoonEpisodeComment_Id(Long userId, Long commentId);

    @Query("SELECT cl.webtoonEpisodeComment.id FROM WebtoonEpisodeCommentLike cl " +
            "WHERE cl.user.id = :userId AND cl.webtoonEpisodeComment.id IN :commentIds")
    List<Long> findLikedCommentIdsByUserIdAndCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds
    );
}
