package com.pageon.backend.dto.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "token")
public class TokenInfo {

    private Long userId;
    private String email;
    private String refreshToken;

    public TokenInfo updateTokenInfo(Long userId, String email, String refreshToken) {
        this.userId = userId;
        this.email = email;
        this.refreshToken = refreshToken;

        return this;
    }

}
