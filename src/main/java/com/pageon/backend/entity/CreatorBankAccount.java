package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.BankCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@ToString
@DynamicUpdate
@Table(name = "creator_bank_accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreatorBankAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @Enumerated(EnumType.STRING)
    private BankCode bankCode;
    private String accountNumber;
    private Boolean isValid;

    public void deleteAccount() {
        this.setDeletedAt(LocalDateTime.now());
        this.isValid = false;
    }

}
