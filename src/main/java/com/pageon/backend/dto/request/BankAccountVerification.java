package com.pageon.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountVerification {
    private String bankCode;
    private String accountNumber;
    private String holderName;
    private String identityNumber;

    public void addCreatorInformation(String holderName, String identityNumber) {
        this.holderName = holderName;
        this.identityNumber = identityNumber;
    }
}
