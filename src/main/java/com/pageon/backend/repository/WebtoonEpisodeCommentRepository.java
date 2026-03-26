package com.pageon.backend.repository;

import com.pageon.backend.entity.WebnovelEpisodeComment;
import com.pageon.backend.entity.WebtoonEpisodeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebtoonEpisodeCommentRepository extends JpaRepository<WebtoonEpisodeComment, Long> {

    @EntityGraph(attributePaths = {"user", "webtoonEpisode", "webtoonEpisode.webtoon"})
    Page<WebtoonEpisodeComment> findAllByWebtoonEpisode_IdAndDeletedAtNull(Long episodeId, Pageable pageable);

    @EntityGraph(attributePaths = {"webtoonEpisode", "webtoonEpisode.webtoon", "user"})
    Page<WebtoonEpisodeComment> findAllByUser_IdAndDeletedAtNull(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "webtoonEpisode"})
    Optional<WebtoonEpisodeComment> findByIdAndUser_Id(Long userId, Long commentId);

    Optional<WebtoonEpisodeComment> findFirstByWebtoonEpisode_IdAndDeletedAtIsNullOrderByLikeCountDescCreatedAtDesc(Long episodeId);

    Long countByWebtoonEpisode_IdAndDeletedAtIsNull(Long episodeId);
}
