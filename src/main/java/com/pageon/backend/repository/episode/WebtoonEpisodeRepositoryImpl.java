package com.pageon.backend.repository.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.dto.response.episode.QEpisodeSummaryResponse;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;

import static com.pageon.backend.entity.QWebtoonEpisode.webtoonEpisode;

@RequiredArgsConstructor
public class WebtoonEpisodeRepositoryImpl implements WebtoonEpisodeRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private BooleanExpression isNotDeleted() {
        return webtoonEpisode.deletedAt.isNull();
    }
    private BooleanExpression isPublished() {
        return webtoonEpisode.episodeStatus.eq(EpisodeStatus.PUBLISHED);
    }

    @Override
    public Slice<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable) {
        List<EpisodeSummaryResponse> responses = queryFactory
                .select(new QEpisodeSummaryResponse(
                        webtoonEpisode.id,
                        webtoonEpisode.episodeNum,
                        webtoonEpisode.episodeTitle,
                        webtoonEpisode.publishedAt,
                        webtoonEpisode.purchasePrice,
                        webtoonEpisode.rentalPrice
                ))
                .from(webtoonEpisode)
                .where(
                        webtoonEpisode.webtoon.id.eq(contentId),
                        isNotDeleted(),
                        isPublished()
                )
                .orderBy(getEpisodeOrder(sort))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
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
            return webtoonEpisode.episodeNum.asc();
        }
        return webtoonEpisode.episodeNum.desc();
    }
}
