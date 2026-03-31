package com.pageon.backend.dto.request.content;

import com.pageon.backend.common.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreate {
    private String title;
    private String description;
    private String contentType;
    private String keywords;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishedAt;
    private WorkStatus workStatus;
}
