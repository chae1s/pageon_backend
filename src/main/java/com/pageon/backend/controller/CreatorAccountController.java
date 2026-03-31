package com.pageon.backend.controller;

import com.pageon.backend.dto.request.BankAccountVerification;
import com.pageon.backend.dto.response.creator.settlement.BankAccount;
import com.pageon.backend.dto.response.creator.settlement.BankAccountList;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.creator.CreatorAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creators/settlements")
@RequiredArgsConstructor
public class CreatorAccountController {

    private final CreatorAccountService creatorAccountService;

    @PostMapping("/bank-account")
    public ResponseEntity<Void> registerBankAccount(
            @AuthenticationPrincipal PrincipalUser principalUser,
            @RequestBody BankAccountVerification request
    ) {

        creatorAccountService.registerBankAccount(principalUser.getId(), request);

        return ResponseEntity.ok().build();

    }

    @GetMapping("/bank-account")
    public ResponseEntity<BankAccount> getBankAccount(@AuthenticationPrincipal PrincipalUser principalUser) {

        BankAccount bankAccount = creatorAccountService.getBankAccount(principalUser.getId());

        return ResponseEntity.ok(bankAccount);
    }

    @GetMapping("/bank-account/history")
    public ResponseEntity<List<BankAccountList>> getMyBankAccountHistory(@AuthenticationPrincipal PrincipalUser principalUser) {
        List<BankAccountList> bankAccountList = creatorAccountService.getMyBankAccountHistory(principalUser.getId());

        return ResponseEntity.ok(bankAccountList);
    }
}
