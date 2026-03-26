package com.pageon.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeCommentRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String text;
    private Boolean isSpoiler;

}
