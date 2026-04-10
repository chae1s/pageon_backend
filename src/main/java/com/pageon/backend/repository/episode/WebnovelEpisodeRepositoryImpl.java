package com.pageon.backend.repository.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.dto.response.episode.QEpisodeSummaryResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

import static com.pageon.backend.entity.QWebnovelEpisode.webnovelEpisode;

@RequiredArgsConstructor
public class WebnovelEpisodeRepositoryImpl implements WebnovelEpisodeRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private BooleanExpression isNotDeleted() {
        return webnovelEpisode.deletedAt.isNull();
    }
    private BooleanExpression isPublished() {
        return webnovelEpisode.episodeStatus.eq(EpisodeStatus.PUBLISHED);
    }
    @Override
    public Page<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable) {
        List<EpisodeSummaryResponse> responses = queryFactory
                .select(new QEpisodeSummaryResponse(
                        webnovelEpisode.id,
                        webnovelEpisode.episodeNum,
                        webnovelEpisode.episodeTitle,
                        webnovelEpisode.publishedAt,
                        webnovelEpisode.purchasePrice,
                        Expressions.nullExpression(Integer.class)
                ))
                .from(webnovelEpisode)
                .where(
                        webnovelEpisode.webnovel.id.eq(contentId),
                        isNotDeleted(),
                        isPublished()
                )
                .orderBy(getEpisodeOrder(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Optional.ofNullable(
                queryFactory
                        .select(webnovelEpisode.count())
                        .from(webnovelEpisode)
                        .where(
                                webnovelEpisode.webnovel.id.eq(contentId),
                                isNotDeleted(),
                                isPublished()
                        )
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(responses, pageable, total);
    }

    @Override
    public Map<Long, List<EpisodeSummaryResponse>> findEpisodeSummariesByContentIds(List<Long> contentIds) {
        return queryFactory
                .select(
                        webnovelEpisode.webnovel.id,
                        webnovelEpisode.id,
                        webnovelEpisode.episodeNum,
                        webnovelEpisode.episodeTitle,
                        webnovelEpisode.publishedAt,
                        webnovelEpisode.purchasePrice,
                        Expressions.nullExpression(Integer.class)
                )
                .from(webnovelEpisode)
                .where(
                        webnovelEpisode.webnovel.id.in(contentIds),
                        isNotDeleted(),
                        isPublished()
                )
                .orderBy(webnovelEpisode.episodeNum.desc())
                .fetch()
                .stream()
                .filter(t -> t.get(webnovelEpisode.webnovel.id) != null)
                .collect(Collectors.groupingBy(
                        t -> Objects.requireNonNull(t.get(webnovelEpisode.webnovel.id)),
                        Collectors.mapping(
                                t -> new EpisodeSummaryResponse(
                                        t.get(webnovelEpisode.id),
                                        t.get(webnovelEpisode.episodeNum),
                                        t.get(webnovelEpisode.episodeTitle),
                                        t.get(webnovelEpisode.publishedAt),
                                        t.get(webnovelEpisode.purchasePrice),
                                        null
                                ),
                                Collectors.toList()
                        )
                ));

    }

    private OrderSpecifier<?> getEpisodeOrder(String sort) {
        if ("first".equalsIgnoreCase(sort)) {
            return webnovelEpisode.episodeNum.asc();
        }
        return webnovelEpisode.episodeNum.desc();
    }
}
