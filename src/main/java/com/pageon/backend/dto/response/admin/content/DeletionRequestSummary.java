package com.pageon.backend.dto.response.admin.content;

import com.pageon.backend.common.enums.DeleteStatus;
import com.pageon.backend.entity.ContentDeletionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionRequestSummary {

    private Long requestId;
    private String contentTitle;
    private String author;
    private DeleteStatus deleteStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static DeletionRequestSummary of(ContentDeletionRequest contentDeletionRequest) {
        return DeletionRequestSummary.builder()
                .requestId(contentDeletionRequest.getId())
                .contentTitle(contentDeletionRequest.getContent().getTitle())
                .author(contentDeletionRequest.getCreator().getPenName())
                .deleteStatus(contentDeletionRequest.getDeleteStatus())
                .requestedAt(contentDeletionRequest.getRequestedAt())
                .processedAt(contentDeletionRequest.getProcessedAt())
                .build();
    }
}
