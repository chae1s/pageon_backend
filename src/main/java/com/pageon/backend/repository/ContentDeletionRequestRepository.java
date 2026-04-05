package com.pageon.backend.repository;

import com.pageon.backend.common.enums.DeleteStatus;
import com.pageon.backend.dto.response.admin.content.DeletionRequestDetail;
import com.pageon.backend.entity.ContentDeletionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ContentDeletionRequestRepository extends JpaRepository<ContentDeletionRequest, Long> {
    @EntityGraph(attributePaths = {"content"})
    Page<ContentDeletionRequest> findByCreatorId(Long creatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"content"})
    Optional<ContentDeletionRequest> findByIdAndCreator_Id(Long deleteId, Long creatorId);

    int countByCreator_IdAndDeleteStatus(Long id, DeleteStatus deleteStatus);

    @EntityGraph(attributePaths = {"content", "creator"})
    Page<ContentDeletionRequest> findAllByDeleteStatus(DeleteStatus deleteStatus, Pageable pageable);

    @Query("SELECT new com.pageon.backend.dto.response.admin.content.DeletionRequestDetail(" +
            "c.id, c.deleteReason, c.reasonDetail) FROM ContentDeletionRequest c " +
            "WHERE c.id = :requestId")
    Optional<DeletionRequestDetail> findRequestById(Long requestId);

    @EntityGraph(attributePaths = {"content"})
    Optional<ContentDeletionRequest> findById(Long requestId);
}
