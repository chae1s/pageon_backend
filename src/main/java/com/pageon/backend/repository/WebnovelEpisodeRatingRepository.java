package com.pageon.backend.repository;

import com.pageon.backend.entity.User;
import com.pageon.backend.entity.WebnovelEpisodeRating;
import com.pageon.backend.entity.WebtoonEpisodeRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WebnovelEpisodeRatingRepository extends JpaRepository<WebnovelEpisodeRating, Long> {

    @Query("SELECT r.score FROM WebnovelEpisodeRating r WHERE r.webnovelEpisode.id = :episodeId AND r.user.id = :userId")
    Integer findScoreByWebnovelEpisodeAndUser(Long episodeId, Long userId);

    @Query("SELECT r FROM WebnovelEpisodeRating r WHERE r.webnovelEpisode.id = :episodeId And r.user.id = :userId")
    Optional<WebnovelEpisodeRating> findByWebnovelEpisodeAndUser(Long episodeId, Long userId);


}
