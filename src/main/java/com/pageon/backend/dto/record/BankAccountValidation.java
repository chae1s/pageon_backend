package com.pageon.backend.dto.record;

public record BankAccountValidation(
        EntityBody entityBody
) {
    public record EntityBody(
            Boolean isValid
    ) {}
}
