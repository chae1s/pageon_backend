package com.pageon.backend.repository;


import com.pageon.backend.entity.WebnovelEpisodeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebnovelEpisodeCommentRepository extends JpaRepository<WebnovelEpisodeComment, Long> {

    @EntityGraph(attributePaths = {"user", "webnovelEpisode", "webnovelEpisode.webnovel"})
    Page<WebnovelEpisodeComment> findAllByWebnovelEpisode_IdAndDeletedAtNull(Long episodeId, Pageable pageable);

    @EntityGraph(attributePaths = {"webnovelEpisode", "webnovelEpisode.webnovel"})
    Page<WebnovelEpisodeComment> findAllByUser_IdAndDeletedAtNull(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "webnovelEpisode"})
    Optional<WebnovelEpisodeComment> findByIdAndUser_Id(Long userId, Long commentId);

    Optional<WebnovelEpisodeComment> findFirstByWebnovelEpisode_IdAndDeletedAtIsNullOrderByLikeCountDescCreatedAtDesc(Long episodeId);

    Long countByWebnovelEpisode_IdAndDeletedAtIsNull(Long episodeId);
}
