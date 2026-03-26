package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.entity.Webnovel;
import com.pageon.backend.entity.Webtoon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContentListResponse {
    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private String penName;
    private List<KeywordResponse> keywords;
    private SerialDay serialDay;
    private SeriesStatus status;
    private Integer episodeCount;
    private Integer averageRating;
    private Long viewCount;

    public static UserContentListResponse fromWebnovel(Webnovel webnovel, List<KeywordResponse> keywords, int episodeCount, int averageRating) {
        UserContentListResponse userContentListResponse = new UserContentListResponse();
        userContentListResponse.setId(webnovel.getId());
        userContentListResponse.setTitle(webnovel.getTitle());
        userContentListResponse.setDescription(webnovel.getDescription());
        userContentListResponse.setCoverUrl(webnovel.getCover());
        userContentListResponse.setPenName(webnovel.getCreator().getPenName());
        userContentListResponse.setKeywords(keywords);
        userContentListResponse.setSerialDay(webnovel.getSerialDay());
        userContentListResponse.setStatus(webnovel.getStatus());
        userContentListResponse.setEpisodeCount(episodeCount);
        userContentListResponse.setAverageRating(averageRating);
        userContentListResponse.setViewCount(webnovel.getViewCount());

        return userContentListResponse;

    }

    public static UserContentListResponse fromWebtoon(Webtoon webtoon, List<KeywordResponse> keywords, int episodeCount, int averageRating) {
        UserContentListResponse userContentListResponse = new UserContentListResponse();
        userContentListResponse.setId(webtoon.getId());
        userContentListResponse.setTitle(webtoon.getTitle());
        userContentListResponse.setDescription(webtoon.getDescription());
        userContentListResponse.setCoverUrl(webtoon.getCover());
        userContentListResponse.setPenName(webtoon.getCreator().getPenName());
        userContentListResponse.setKeywords(keywords);
        userContentListResponse.setSerialDay(webtoon.getSerialDay());
        userContentListResponse.setStatus(webtoon.getStatus());
        userContentListResponse.setEpisodeCount(episodeCount);
        userContentListResponse.setAverageRating(averageRating);
        userContentListResponse.setViewCount(webtoon.getViewCount());

        return userContentListResponse;

    }

}
