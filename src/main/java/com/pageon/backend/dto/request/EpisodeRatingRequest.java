package com.pageon.backend.dto.request;

import com.pageon.backend.common.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeRatingRequest {

    @NotBlank(message = "평점을 선택해주세요.")
    private Integer score;


}
