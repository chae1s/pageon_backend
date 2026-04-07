package com.pageon.backend.service.creator;

import com.pageon.backend.entity.WebnovelEpisode;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.repository.WebnovelEpisodeRepository;
import com.pageon.backend.repository.WebtoonEpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EpisodeUpdateService {
    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;

    @Transactional
    public List<WebnovelEpisode> updateWebnovelEpisodeStatus(LocalDate publishedAt) {
        List<WebnovelEpisode> webnovelEpisodes = webnovelEpisodeRepository.findAllByPublishedAt(publishedAt);
        webnovelEpisodes.forEach(EpisodeBase::publish);

        return webnovelEpisodes;
    }

    @Transactional
    public List<WebtoonEpisode> updateWebtoonEpisodeStatus(LocalDate publishedAt) {
        List<WebtoonEpisode> webtoonEpisodes = webtoonEpisodeRepository.findAllByPublishedAt(publishedAt);
        webtoonEpisodes.forEach(EpisodeBase::publish);

        return webtoonEpisodes;
    }
}
