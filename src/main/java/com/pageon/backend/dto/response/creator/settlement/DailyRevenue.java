package com.pageon.backend.dto.response.creator.settlement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenue {
    private LocalDate date;
    private Long revenue;
}
