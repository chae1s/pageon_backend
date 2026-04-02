package com.pageon.backend.dto.response.creator.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContentRevenue {
    private String contentTitle;
    private Long revenue;
    private Double percentage;
}
