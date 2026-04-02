package com.pageon.backend.service.creator;

import com.pageon.backend.client.payment.TossPaymentClient;
import com.pageon.backend.common.enums.BankCode;
import com.pageon.backend.common.utils.AesEncryptionUtil;
import com.pageon.backend.dto.record.BankAccountValidation;
import com.pageon.backend.dto.request.BankAccountVerification;
import com.pageon.backend.dto.response.creator.settlement.BankAccount;
import com.pageon.backend.dto.response.creator.settlement.BankAccountList;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.CreatorBankAccount;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.repository.creator.CreatorBankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorAccountService {

    private final CreatorBankAccountRepository creatorBankaccountRepository;
    private final CreatorRepository creatorRepository;
    private final TossPaymentClient tossPaymentClient;
    private final AesEncryptionUtil aesEncryptionUtil;


    @Transactional
    public void registerBankAccount(Long userId, BankAccountVerification request) {
        Creator creator = creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );

        String holderName = creator.getUser().getName();
        LocalDate birthDate = creator.getUser().getBirthDate();
        String identityNumber = birthDate.format(DateTimeFormatter.ofPattern("yyMMdd"));

        request.addCreatorInformation(holderName, identityNumber);

        BankCode bankCode =BankCode.fromCode(request.getBankCode());

        BankAccountValidation validation = tossPaymentClient.validateAccount(request);

        if (!validation.entityBody().isValid()) {
            throw new CustomException(ErrorCode.INVALID_BANK_ACCOUNT);
        }

        checkExistingAccount(creator.getId());

        CreatorBankAccount creatorBankAccount = CreatorBankAccount.builder()
                .creator(creator)
                .accountNumber(aesEncryptionUtil.encrypt(request.getAccountNumber()))
                .bankCode(bankCode)
                .isValid(true)
                .build();

        creatorBankaccountRepository.save(creatorBankAccount);
    }

    private void checkExistingAccount(Long creatorId) {
        creatorBankaccountRepository.findByCreator_IdAndDeletedAtIsNull(creatorId).ifPresent(
                CreatorBankAccount::deleteAccount
        );
    }

    public BankAccount getBankAccount(Long userId) {
        Creator creator = creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );

        CreatorBankAccount creatorBankAccount = creatorBankaccountRepository.findByCreator_IdAndDeletedAtIsNull(creator.getId()).orElse(null);

        if (creatorBankAccount == null) {
            return new BankAccount();
        }

        String accountNumber = aesEncryptionUtil.decrypt((creatorBankAccount.getAccountNumber()));

        return BankAccount.of(creatorBankAccount, maskAccountNumber(accountNumber));
    }

    public List<BankAccountList> getMyBankAccountHistory(Long userId) {
        Creator creator = creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );

        return creatorBankaccountRepository.findAllByCreatorId(creator.getId());
    }

    private String maskAccountNumber(String accountNumber) {
        String visibleFront = accountNumber.substring(0, 4);
        String visibleBack = accountNumber.substring(accountNumber.length() - 4);

        String masked = "*".repeat(accountNumber.length() - 8);

        return visibleFront + masked + visibleBack;
    }
}
