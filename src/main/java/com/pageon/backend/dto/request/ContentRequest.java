package com.pageon.backend.dto.request;

import com.pageon.backend.common.enums.DeleteReason;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class ContentRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String title;
        private String description;
        private String contentType;
        private String keywords;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate publishedAt;
        private MultipartFile coverImage;
        private WorkStatus workStatus;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String title;
        private String description;
        private String keywords;
        private SerialDay serialDay;
        private MultipartFile coverImage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delete {
        private DeleteReason deleteReason;
        private String reasonDetail;
    }
}
