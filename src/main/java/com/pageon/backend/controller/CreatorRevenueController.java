package com.pageon.backend.controller;

import com.pageon.backend.dto.response.creator.settlement.*;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.creator.CreatorRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/creators/revenue")
@RequiredArgsConstructor
public class CreatorRevenueController {

    private final CreatorRevenueService creatorRevenueService;

    @GetMapping("/latest")
    public ResponseEntity<RevenueDetail> getLatestRevenue(@AuthenticationPrincipal PrincipalUser principalUser) {
        RevenueDetail revenue = creatorRevenueService.getLatestRevenue(principalUser.getId());

        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<RevenueDashboard> getRevenueDashboard(@AuthenticationPrincipal PrincipalUser principalUser) {
        RevenueDashboard revenueDashboard = creatorRevenueService.getRevenueDashboard(principalUser.getId());

        return ResponseEntity.ok(revenueDashboard);
    }

    @GetMapping("/analytics")
    public ResponseEntity<RevenueAnalytics> getRevenueAnalytics(@AuthenticationPrincipal PrincipalUser principalUser) {
        RevenueAnalytics revenueAnalytics = creatorRevenueService.getRevenueAnalytics(principalUser.getId());

        return ResponseEntity.ok(revenueAnalytics);
    }

    @GetMapping("/daily")
    public ResponseEntity<RevenueTrend> getDailyRevenue(@AuthenticationPrincipal PrincipalUser principalUser) {
        RevenueTrend revenueTrend = creatorRevenueService.getDailyRevenue(principalUser.getId());

        return ResponseEntity.ok(revenueTrend);
    }
}
