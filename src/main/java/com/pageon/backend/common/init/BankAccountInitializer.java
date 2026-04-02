package com.pageon.backend.common.init;

import com.pageon.backend.common.enums.BankCode;
import com.pageon.backend.common.utils.AesEncryptionUtil;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.CreatorBankAccount;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.repository.creator.CreatorBankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class BankAccountInitializer implements CommandLineRunner {

    private final CreatorRepository creatorRepository;
    private final CreatorBankAccountRepository creatorBankAccountRepository;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final Random random = new Random();

    private static final BankCode[] BANK_CODES = BankCode.values();

    @Override
    public void run(String... args) throws Exception {
        Long count = creatorBankAccountRepository.count();
        if (count > 0) {
            log.info("이미 계좌 데이터가 존재합니다. BankAccountInitializer skip");
            return;
        }

        log.info("=== BankAccountInitializer 시작 ===");

        List<Creator> creators = creatorRepository.findAll(PageRequest.of(0, 12000)).getContent();

        List<CreatorBankAccount> accounts = new ArrayList<>(creators.size());

        for (Creator creator : creators) {
            String rawAccountNumber = generateAccountNumber(creator.getId());
            String encryptedAccountNumber = aesEncryptionUtil.encrypt(rawAccountNumber);
            BankCode bankCode = BANK_CODES[random.nextInt(BANK_CODES.length)];

            accounts.add(CreatorBankAccount.builder()
                    .creator(creator)
                    .bankCode(bankCode)
                    .accountNumber(encryptedAccountNumber)
                    .isValid(true)
                    .build());
        }

        creatorBankAccountRepository.saveAll(accounts);

        log.info("=== BankAccountInitializer 완료 - 총 {}개 ===", accounts.size());
    }

    private String generateAccountNumber(Long creatorId) {
        StringBuilder sb = new StringBuilder(String.valueOf(creatorId));
        while (sb.length() < 12) {
            sb.append(random.nextInt(10));
        }
        return sb.substring(0, 12);
    }
}