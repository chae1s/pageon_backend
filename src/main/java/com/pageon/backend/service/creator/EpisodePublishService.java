package com.pageon.backend.service.creator;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.NotificationType;
import com.pageon.backend.dto.record.EpisodeNotificationEvent;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.WebnovelEpisode;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.repository.InterestRepository;
import com.pageon.backend.service.kafka.NotificationEventProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpisodePublishService {

    private final EpisodeUpdateService episodeUpdateService;
    private final NotificationEventProducer producer;
    private final InterestRepository interestRepository;


    public void publishScheduledWebnovelEpisodes(LocalDate publishedAt) {
        List<WebnovelEpisode> webnovelEpisodes = episodeUpdateService.updateWebnovelEpisodeStatus(publishedAt);

        if (webnovelEpisodes.isEmpty()) {
            return;
        }

        LocalDateTime eventTime = LocalDateTime.now();

        Set<Long> webnovelIds = webnovelEpisodes.stream()
                .map(e -> e.getWebnovel().getId())
                .collect(Collectors.toSet());

        Map<Long, List<Long>> interestMap = getInterestMap(webnovelIds);

        for (WebnovelEpisode episode : webnovelEpisodes) {

            sendKafkaProducer(
                    episode.getParentContent(),
                    interestMap.getOrDefault(episode.getWebnovel().getId(), Collections.emptyList()),
                    eventTime
            );
        }

    }

    public void publishScheduledWebtoonEpisodes(LocalDate publishedAt) {
        List<WebtoonEpisode> webtoonEpisodes = episodeUpdateService.updateWebtoonEpisodeStatus(publishedAt);

        if (webtoonEpisodes.isEmpty()) {
            return;
        }

        LocalDateTime eventTime = LocalDateTime.now();

        Set<Long> webtoonIds = webtoonEpisodes.stream()
                .map(e -> e.getWebtoon().getId())
                .collect(Collectors.toSet());

        Map<Long, List<Long>> interestMap = getInterestMap(webtoonIds);

        for (WebtoonEpisode episode : webtoonEpisodes) {

            sendKafkaProducer(
                    episode.getParentContent(),
                    interestMap.getOrDefault(episode.getWebtoon().getId(), Collections.emptyList()),
                    eventTime
            );

        }

    }


    private Map<Long, List<Long>> getInterestMap(Set<Long> contentIds) {

        return interestRepository.findUserIdsByContentIds(contentIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(
                                row -> (Long) row[1],
                                Collectors.toList()
                        )
                ));

    }

    private void sendKafkaProducer(Content content, List<Long> userIds, LocalDateTime eventTime) {

        userIds.forEach(userId ->
                producer.sendEpisodeMessage(
                        new EpisodeNotificationEvent(
                                userId,
                                NotificationType.NEW_EPISODE_RELEASED,
                                ContentType.valueOf(content.getDtype()),
                                content.getId(), content.getTitle(),
                                eventTime
                        )
                )
        );

        producer.sendEpisodeMessage(new EpisodeNotificationEvent(
                content.getCreator().getUser().getId(),
                NotificationType.SCHEDULED_EPISODE_POSTED,
                ContentType.valueOf(content.getDtype()),
                content.getId(), content.getTitle(),
                eventTime
        ));
    }


}
