package com.pageon.backend.controller;

import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.admin.content.DeletionRequestDetail;
import com.pageon.backend.dto.response.admin.content.DeletionRequestSummary;
import com.pageon.backend.service.admin.AdminDeletionRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/admin/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminDeletionRequestService adminDeletionRequestService;

    @GetMapping("/deletion-requests")
    public ResponseEntity<PageResponse<DeletionRequestSummary>> getAllDeletionRequests(
            @RequestParam String status, @PageableDefault(size = 60) Pageable pageable
    ) {
        Page<DeletionRequestSummary> deletionRequests = adminDeletionRequestService.getAllDeletionRequests(status, pageable);

        return ResponseEntity.ok(new PageResponse<>(deletionRequests));
    }

    @GetMapping("/deletion-requests/{requestId}")
    public ResponseEntity<DeletionRequestDetail> getDeletionRequestDetail(@PathVariable Long requestId) {

        DeletionRequestDetail detail = adminDeletionRequestService.getDeletionRequestDetail(requestId);

        return ResponseEntity.ok(detail);
    }

    @PatchMapping("/deletion-requests/{requestId}/approval")
    public ResponseEntity<Void> approveDeletionRequest(@PathVariable Long requestId) {

        adminDeletionRequestService.approveDeletionRequest(requestId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/deletion-requests/{requestId}/rejection")
    public ResponseEntity<Void> rejectDeletionRequest(@PathVariable Long requestId) {

        adminDeletionRequestService.rejectDeletionRequest(requestId);

        return ResponseEntity.ok().build();
    }

}
