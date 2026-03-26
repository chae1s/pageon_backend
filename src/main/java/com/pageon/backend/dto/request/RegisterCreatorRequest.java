package com.pageon.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCreatorRequest {
    @NotBlank(message = "필명을 입력해주세요.")
    private String penName;
    @NotBlank(message = "타입을 선택해주세요.")
    private String contentType;
    @NotNull(message = "약관에 동의해주세요.")
    private Boolean agreedToAiPolicy;
}
