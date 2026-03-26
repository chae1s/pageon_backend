package com.pageon.backend.repository;

import com.pageon.backend.entity.WebtoonEpisodeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WebtoonEpisodeRatingRepository extends JpaRepository<WebtoonEpisodeRating, Long> {


    @Query("SELECT r.score FROM WebtoonEpisodeRating r WHERE r.webtoonEpisode.id = :episodeId AND r.user.id = :userId")
    Integer findScoreByWebtoonEpisodeAndUser(Long episodeId, Long userId);

    @Query("SELECT r FROM WebtoonEpisodeRating r WHERE r.webtoonEpisode.id = :episodeId And r.user.id = :userId")
    Optional<WebtoonEpisodeRating> findByWebtoonEpisodeAndUser(Long episodeId, Long userId);


}
