package com.pageon.backend.common.enums;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;

public enum SerialDay {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;

    public static SerialDay from(String value) {
        try {
            return SerialDay.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_SERIAL_DAY);
        }
    }
}
