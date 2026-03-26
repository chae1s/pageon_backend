package com.pageon.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;
    @NotBlank(message = "설명을 입력해주세요.")
    private String description;
    @NotBlank(message = "키워드를 입력해주세요.")
    private String keywords;
    private MultipartFile coverFile;
    @NotBlank(message = "연재 요일을 입력해주세요.")
    private String serialDay;
}
