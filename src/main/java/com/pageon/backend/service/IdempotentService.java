package com.pageon.backend.service;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IdempotentService {

    private final String VALUE = "IDEMPOTENT";
    private final RedisTemplate<String, String> redisTemplate;
    private final Integer TIME_LIMIT = 2;

    public void isValidIdempotent(List<String> keyElement) {
        String idempotentKey = String.join(":", keyElement);

        Boolean isSuccess = redisTemplate.opsForValue().setIfAbsent(idempotentKey, VALUE, TIME_LIMIT, TimeUnit.SECONDS);

        if (!isSuccess) {
            throw new CustomException(ErrorCode.DUPLICATION_REQUEST);
        }

    }


}
