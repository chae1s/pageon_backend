package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.Gender;
import com.pageon.backend.common.enums.IdentityProvider;
import com.pageon.backend.common.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@DynamicUpdate
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            unique = true,
            nullable = false
    )
    private String email;

    private String password;

    @Column(
            unique = true,
            nullable = false
    )
    private String nickname;
    private LocalDate birthDate;
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Builder.Default
    private Integer pointBalance = 0;

    // email, kakao, naver, google
    @Enumerated(EnumType.STRING)
    private OAuthProvider oAuthProvider;

    // 소셜 로그인 시 제공받는 id
    @Column(unique = true)
    private String providerId;

    @Column(nullable = false)
    private Boolean termsAgreed;

    // 본인인증 추가 정보
    private String name;
    @Column(unique = true)
    private String phoneNumber;
    private String di;
    @Builder.Default
    private Boolean isPhoneVerified = false;
    @Enumerated(EnumType.STRING)
    private IdentityProvider identityProvider;

    private String customerKey;


    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Creator creator;

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<Interest> interests = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<PointTransaction> pointTransactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<WebtoonEpisodeRating> webtoonEpisodeRatings = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<WebnovelEpisodeRating> webnovelEpisodeRatings = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<WebnovelEpisodeComment> webnovelEpisodeComments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<WebtoonEpisodeComment> webtoonEpisodeComments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user")
    private List<EpisodePurchase> episodePurchases = new ArrayList<>();


    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserRole> userRoles = new ArrayList<>();

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void delete() {
        this.setDeletedAt(LocalDateTime.now());
    }

    public void deleteEmail(String deleteEmail) {
        this.email = deleteEmail;
    }

    public void deleteProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void updateIdentityVerification(String name, String phoneNumber, LocalDate birthDate, Gender gender, String di, boolean isPhoneVerified, IdentityProvider identityProvider) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.gender = gender;
        this.di = di;
        this.isPhoneVerified = isPhoneVerified;
        this.identityProvider = identityProvider;
    }

    public User(String email, String password, String nickname, LocalDate birthDate, Integer pointBalance, OAuthProvider oAuthProvider, Gender gender) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.pointBalance = pointBalance;
        this.oAuthProvider = oAuthProvider;
        this.userRoles = new ArrayList<>();
        this.termsAgreed = true;
        this.isPhoneVerified = false;
        this.gender = gender;
    }

    public void changePoints(int amount) {
        this.pointBalance += amount;
    }

    public void assignCustomerKey(String customerKey) {
        this.customerKey = customerKey;
    }


}
