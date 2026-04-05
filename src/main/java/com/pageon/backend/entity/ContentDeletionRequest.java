package com.pageon.backend.entity;

import com.pageon.backend.common.enums.DeleteReason;
import com.pageon.backend.common.enums.DeleteStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "content_deletion_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ContentDeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
    @Enumerated(EnumType.STRING)
    private DeleteReason deleteReason;
    private String reasonDetail;
    @Enumerated(EnumType.STRING)
    private DeleteStatus deleteStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;


    public void cancelDeletion() {
        this.deleteStatus = DeleteStatus.CANCELED;
        this.processedAt = LocalDateTime.now();
    }

    public void approveDeletion() {
        this.deleteStatus = DeleteStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void rejectDeletion() {
        this.deleteStatus = DeleteStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }


}
