package com.pageon.backend.repository;

import com.pageon.backend.entity.ContentDeletionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentDeletionRequestRepository extends JpaRepository<ContentDeletionRequest, Long> {
    @EntityGraph(attributePaths = {"content"})
    Page<ContentDeletionRequest> findByCreatorId(Long creatorId, Pageable pageable);

    @EntityGraph(attributePaths = {"content"})
    Optional<ContentDeletionRequest> findByIdAndCreator_Id(Long deleteId, Long creatorId);
}
