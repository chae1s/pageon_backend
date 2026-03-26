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


    public TokenInfo updateTokenInfo(Long userId, String email) {
        this.userId = userId;
        this.email = email;

        return this;
    }

}
