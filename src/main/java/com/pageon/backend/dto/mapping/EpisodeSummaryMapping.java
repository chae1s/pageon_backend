package com.pageon.backend.dto.mapping;

import java.time.LocalDate;

public interface EpisodeSummaryMapping {
    Long getContentId();
    Long getEpisodeId();
    Integer getEpisodeNum();
    String getEpisodeTitle();
    LocalDate getPublishedAt();
    Integer getPurchasePrice();
    Integer getRentalPrice();
}
