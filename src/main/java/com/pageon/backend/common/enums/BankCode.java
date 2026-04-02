package com.pageon.backend.common.enums;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;

import java.util.Arrays;

public enum BankCode {
    한국은행("001"), KDB산업은행("002"), IBK기업은행("003"), 국민은행("004"), 수협("007"),
    NH농협은행("011"), 우리은행("020"), SC제일은행("023"), 씨티은행("027"), 대구은행("031"),
    부산은행("032"), 광주은행("034"), 제주은행("035"), 전북은행("037"), 경남은행("039"),
    우체국("071"), 하나은행("081"), 신한은행("088"), 카카오뱅크("090"), 토스뱅크("092");


    private final String code;

    private BankCode(String code) {
        this.code = code;
    }

    public static BankCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(b -> b.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_BANK_CODE));
    }

    public String getBankCodeNum() {
        return code;
    }


}
