package com.pageon.backend.dto.request.episode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebnovelEpisodeUpdate {
    private String title;
    private LocalDate publishedAt;
    private String content;
}
