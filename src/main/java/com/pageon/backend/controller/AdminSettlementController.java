package com.pageon.backend.controller;

import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.admin.settlement.SettlementDetail;
import com.pageon.backend.dto.response.admin.settlement.SettlementSummary;
import com.pageon.backend.service.admin.AdminSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {
    private final AdminSettlementService adminSettlementService;

    @GetMapping()
    public ResponseEntity<PageResponse<SettlementSummary>> getSettlementsByStatus(
            @RequestParam Integer month,
            @PageableDefault(size = 60) Pageable pageable, @RequestParam String status
    ) {
        Page<SettlementSummary> settlements = adminSettlementService.getSettlementStatusByStatus(month, pageable, status);

        return ResponseEntity.ok(new PageResponse<>(settlements));
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementDetail> getSettlementById(@PathVariable Long settlementId) {
        SettlementDetail settlement = adminSettlementService.getSettlementDetail(settlementId);

        return ResponseEntity.ok(settlement);
    }

    @PatchMapping("/{settlementId}/payout-retry")
    public ResponseEntity<Void> retryPayout(@PathVariable Long settlementId, @RequestParam Long creatorId) {
        adminSettlementService.retryPayout(settlementId, creatorId);

        return ResponseEntity.ok().build();
    }
}