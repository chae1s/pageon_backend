package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.entity.Webtoon;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorWebtoonResponse {
    private Long id;
    private String title;
    private String description;
    private List<CreatorKeywordResponse> keywords;
    private SeriesStatus status;
    private String cover;
    private SerialDay serialDay;


    public static CreatorWebtoonResponse fromEntity(Webtoon webtoon, List<CreatorKeywordResponse> keywords) {
        CreatorWebtoonResponse creatorContentResponse = new CreatorWebtoonResponse();
        creatorContentResponse.setId(webtoon.getId());
        creatorContentResponse.setTitle(webtoon.getTitle());
        creatorContentResponse.setDescription(webtoon.getDescription());
        creatorContentResponse.setKeywords(keywords);
        creatorContentResponse.setStatus(webtoon.getStatus());
        creatorContentResponse.setCover(webtoon.getCover());
        creatorContentResponse.setSerialDay(webtoon.getSerialDay());

        return creatorContentResponse;

    }

}
