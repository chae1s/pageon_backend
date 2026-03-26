package com.pageon.backend.repository;

import com.pageon.backend.entity.WebtoonImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebtoonImageRepository extends JpaRepository<WebtoonImage, Long> {

    List<WebtoonImage> findByWebtoonEpisodeIdOrderBySequenceAsc(Long episodeId);
}
