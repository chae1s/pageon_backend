package com.pageon.backend.repository.content;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.dto.response.content.KeywordResponse;
import com.pageon.backend.dto.response.content.QContentDetailResponse;
import com.pageon.backend.dto.response.content.QKeywordResponse;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.pageon.backend.entity.QContent.content;
import static com.pageon.backend.entity.QContentKeyword.contentKeyword;
import static com.pageon.backend.entity.QKeyword.keyword;

@RequiredArgsConstructor
public class ContentRepositoryImpl implements ContentRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private BooleanExpression isNotDeleted() {
        return content.deletedAt.isNull();
    }
    private BooleanExpression isPublished() {
        return content.workStatus.eq(WorkStatus.PUBLISHED);
    }

    @Override
    public Optional<ContentDetailResponse> findContentDetail(Long contentId) {
        ContentDetailResponse response = queryFactory
                .select(new QContentDetailResponse(
                        content.id,
                        content.dtype,
                        content.title,
                        content.cover,
                        content.creator.penName,
                        content.description,
                        content.serialDay,
                        content.status,
                        content.totalAverageRating,
                        content.totalRatingCount,
                        content.viewCount
                ))
                .from(content)
                .join(content.creator)
                .where(
                        content.id.eq(contentId),
                        isNotDeleted(),
                        isPublished()
                )
                .fetchOne();

        if (response != null) {
            List<KeywordResponse> keywords = queryFactory
                    .select(new QKeywordResponse(
                            keyword.id,
                            keyword.name
                    ))
                    .from(contentKeyword)
                    .join(contentKeyword.keyword, keyword)
                    .where(contentKeyword.content.id.eq(contentId))
                    .fetch();

            response.setKeywords(keywords);
        }

        return Optional.ofNullable(response);
    }
}
