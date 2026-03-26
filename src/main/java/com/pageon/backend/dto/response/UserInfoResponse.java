package com.pageon.backend.dto.response;

import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserInfoResponse {
    private Long id;
    private String email;
    private String nickname;
    private LocalDate birthDate;
    private Integer pointBalance;
    private OAuthProvider oAuthProvider;

    public static UserInfoResponse fromEntity(User user) {
        UserInfoResponse userInfoResponse = new UserInfoResponse();
        userInfoResponse.setId(user.getId());
        userInfoResponse.setEmail(user.getEmail());
        userInfoResponse.setNickname(user.getNickname());
        userInfoResponse.setBirthDate(user.getBirthDate());
        userInfoResponse.setPointBalance(user.getPointBalance());
        userInfoResponse.setOAuthProvider(user.getOAuthProvider());
        return userInfoResponse;
    }
}
