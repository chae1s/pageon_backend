package com.pageon.backend.repository.episode;

import com.pageon.backend.dto.response.episode.EpisodePurchaseResponse;
import com.pageon.backend.dto.response.episode.QEpisodePurchaseResponse;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.pageon.backend.entity.QEpisodePurchase.episodePurchase;
@RequiredArgsConstructor
public class EpisodePurchaseRepositoryImpl implements EpisodePurchaseRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EpisodePurchaseResponse> findEpisodePurchases(Long userId, List<Long> episodeIds) {
        return queryFactory
                .select(new QEpisodePurchaseResponse(
                        episodePurchase.episodeId,
                        episodePurchase.purchaseType,
                        episodePurchase.expiredAt
                ))
                .from(episodePurchase)
                .where(
                        episodePurchase.user.id.eq(userId),
                        episodePurchase.episodeId.in(episodeIds)
                )
                .fetch();

    }
}
