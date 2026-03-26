package com.pageon.backend.repository;

import com.pageon.backend.entity.WebnovelEpisodeCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebnovelEpisodeCommentLikeRepository extends JpaRepository<WebnovelEpisodeCommentLike,Long> {

    Boolean existsByUser_IdAndWebnovelEpisodeComment_Id(Long userId, Long commentId);

    Optional<WebnovelEpisodeCommentLike> findByUser_IdAndWebnovelEpisodeComment_Id(Long userId, Long commentId);

    @Query("SELECT cl.webnovelEpisodeComment.id FROM WebnovelEpisodeCommentLike cl " +
            "WHERE cl.user.id = :userId AND cl.webnovelEpisodeComment.id IN :commentIds")
    List<Long> findLikedCommentIdsByUserIdAndCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds
    );
}
