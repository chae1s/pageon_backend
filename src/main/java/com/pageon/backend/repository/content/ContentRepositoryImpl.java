package com.pageon.backend.repository.content;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.dto.response.content.KeywordResponse;
import com.pageon.backend.dto.response.content.QContentDetailResponse;
import com.pageon.backend.dto.response.content.QKeywordResponse;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public List<ContentDetailResponse> findContentDetails(SerialDay serialDay) {
        List<ContentDetailResponse> responses = queryFactory
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
                        content.serialDay.eq(serialDay),
                        isNotDeleted(),
                        isPublished()
                )
                .orderBy(content.viewCount.desc())
                .limit(5000)
                .fetch();
        if (!responses.isEmpty()) {
            List<Long> contentIds = responses.stream()
                    .map(ContentDetailResponse::getContentId)
                    .toList();

            List<Tuple> keywords = queryFactory
                    .select(
                            contentKeyword.content.id,  // contentId 포함
                            keyword.id,
                            keyword.name
                    )
                    .from(contentKeyword)
                    .join(contentKeyword.keyword, keyword)
                    .where(contentKeyword.content.id.in(contentIds))
                    .fetch();

            Map<Long, List<KeywordResponse>> keywordMap = keywords.stream()
                    .filter(t -> t.get(contentKeyword.content.id) != null)
                    .collect(Collectors.groupingBy(
                            t -> Objects.requireNonNull(t.get(contentKeyword.content.id)),
                            Collectors.mapping(
                                    t -> new KeywordResponse(
                                            t.get(keyword.id),
                                            t.get(keyword.name)
                                    ),
                                    Collectors.toList()
                            )
                    ));

            responses.forEach(response ->
                    response.setKeywords(keywordMap.getOrDefault(response.getContentId(), List.of()))
            );
        }

        return responses;
    }
}
