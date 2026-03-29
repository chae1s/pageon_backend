package com.pageon.backend.dto.request.episode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebtoonEpisodeCreate {
    @NotBlank
    private String title;
    @NotNull
    private LocalDate publishedAt;

}
