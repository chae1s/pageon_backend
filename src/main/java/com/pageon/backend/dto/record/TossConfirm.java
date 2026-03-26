package com.pageon.backend.dto.record;

public record TossConfirm(
        String mId,
        String method,
        String approvedAt,
        EasyPay easyPay
) {
    public record EasyPay(
            String provider
    ) {}
}