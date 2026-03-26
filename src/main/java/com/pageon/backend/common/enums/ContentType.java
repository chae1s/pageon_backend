package com.pageon.backend.common.enums;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;

public enum ContentType {
    WEBTOON, WEBNOVEL;

    public static ContentType fromUrlPath(String path) {
        return switch (path) {
            case "webnovels" -> WEBNOVEL;
            case "webtoons" -> WEBTOON;
            default -> throw new CustomException(ErrorCode.INVALID_CONTENT_TYPE);
        };
    }
}
