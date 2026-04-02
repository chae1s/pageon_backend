package com.pageon.backend.dto.response.creator.settlement;

import com.pageon.backend.common.enums.BankCode;
import com.pageon.backend.entity.CreatorBankAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountList {
    private Long id;
    private BankCode bankCode;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public static BankAccountList of(CreatorBankAccount creatorBankAccount) {
        return BankAccountList.builder()
                .id(creatorBankAccount.getId())
                .bankCode(creatorBankAccount.getBankCode())
                .createdAt(creatorBankAccount.getCreatedAt())
                .deletedAt(creatorBankAccount.getDeletedAt())
                .build();
    }
}
