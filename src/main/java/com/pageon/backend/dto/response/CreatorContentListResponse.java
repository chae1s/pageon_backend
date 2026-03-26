package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.entity.Webnovel;
import com.pageon.backend.entity.Webtoon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorContentListResponse {
    private Long id;
    private String title;
    private SeriesStatus status;
    private String cover;
    private SerialDay serialDay;

    public static CreatorContentListResponse fromWebnovel(Webnovel webnovel) {
        CreatorContentListResponse creatorContentListResponse = new CreatorContentListResponse();
        creatorContentListResponse.setId(webnovel.getId());
        creatorContentListResponse.setTitle(webnovel.getTitle());
        creatorContentListResponse.setStatus(webnovel.getStatus());
        creatorContentListResponse.setCover(webnovel.getCover());
        creatorContentListResponse.setSerialDay(webnovel.getSerialDay());

        return creatorContentListResponse;

    }

    public static CreatorContentListResponse fromWebtoon(Webtoon webtoon) {
        CreatorContentListResponse creatorContentListResponse = new CreatorContentListResponse();
        creatorContentListResponse.setId(webtoon.getId());
        creatorContentListResponse.setTitle(webtoon.getTitle());
        creatorContentListResponse.setStatus(webtoon.getStatus());
        creatorContentListResponse.setCover(webtoon.getCover());
        creatorContentListResponse.setSerialDay(webtoon.getSerialDay());

        return creatorContentListResponse;

    }
}
