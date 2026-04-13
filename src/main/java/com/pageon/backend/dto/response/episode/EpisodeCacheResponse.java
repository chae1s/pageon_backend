package com.pageon.backend.dto.response.episode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeCacheResponse {

    private List<EpisodeSummaryResponse> episodes;
    private boolean hasNext;


}
