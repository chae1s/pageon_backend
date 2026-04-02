package com.pageon.backend.dto.response.creator.settlement;

import com.pageon.backend.dto.response.creator.content.ContentRevenue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalytics {
    private Long totalRevenue;
    private List<ContentRevenue> contents;
}
