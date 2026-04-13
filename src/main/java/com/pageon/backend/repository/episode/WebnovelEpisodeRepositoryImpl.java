package com.pageon.backend.repository.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.dto.response.episode.QEpisodeSummaryResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;

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
    public Slice<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable) {

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
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = false;
        if (responses.size() > pageable.getPageSize()) {
            responses.remove(pageable.getPageSize());
            hasNext = true;
        }

        return new SliceImpl<>(responses, pageable, hasNext);
    }


    private OrderSpecifier<?> getEpisodeOrder(String sort) {
        if ("first".equalsIgnoreCase(sort)) {
            return webnovelEpisode.episodeNum.asc();
        }
        return webnovelEpisode.episodeNum.desc();
    }
}
