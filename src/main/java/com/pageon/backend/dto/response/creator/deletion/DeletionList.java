package com.pageon.backend.dto.response.creator.deletion;

import com.pageon.backend.common.enums.DeleteReason;
import com.pageon.backend.common.enums.DeleteStatus;
import com.pageon.backend.entity.ContentDeletionRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionList {
    private Long id;
    private String contentTitle;
    private DeleteReason deleteReason;
    private String reasonDetail;
    private LocalDateTime requestedAt;
    private DeleteStatus deleteStatus;

    public static DeletionList fromEntity(ContentDeletionRequest contentDelete) {
        return DeletionList.builder()
                .id(contentDelete.getId())
                .contentTitle(contentDelete.getContent().getTitle())
                .deleteReason(contentDelete.getDeleteReason())
                .reasonDetail(contentDelete.getReasonDetail())
                .requestedAt(contentDelete.getRequestedAt())
                .deleteStatus(contentDelete.getDeleteStatus())
                .build();
    }
}
