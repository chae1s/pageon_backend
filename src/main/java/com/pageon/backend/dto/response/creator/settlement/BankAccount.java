package com.pageon.backend.dto.response.creator.settlement;

import com.pageon.backend.common.enums.BankCode;
import com.pageon.backend.entity.CreatorBankAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccount {
    private Long id;
    private BankCode bankCode;
    private String accountNumber;

    public static BankAccount of(CreatorBankAccount creatorBankAccount, String accountNumber) {
        return BankAccount.builder()
                .id(creatorBankAccount.getId())
                .bankCode(creatorBankAccount.getBankCode())
                .accountNumber(accountNumber)
                .build();
    }
}
