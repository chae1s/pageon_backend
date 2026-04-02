package com.pageon.backend.repository.creator;

import com.pageon.backend.dto.response.creator.settlement.BankAccountList;
import com.pageon.backend.entity.CreatorBankAccount;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CreatorBankAccountRepository extends JpaRepository<CreatorBankAccount,Integer> {

    Optional<CreatorBankAccount> findByCreator_IdAndDeletedAtIsNull(Long id);

    @Query("SELECT new com.pageon.backend.dto.response.creator.settlement.BankAccountList(" +
            "c.id, c.bankCode, c.createdAt, c.deletedAt) FROM CreatorBankAccount c " +
            "WHERE c.creator.id = :creatorId")
    List<BankAccountList> findAllByCreatorId(Long creatorId);


    List<CreatorBankAccount> findAllByCreatorIdInAndIsValidTrue(List<Long> creatorIds);
}
