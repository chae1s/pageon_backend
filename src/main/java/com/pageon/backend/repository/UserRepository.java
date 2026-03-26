package com.pageon.backend.repository;

import com.pageon.backend.entity.User;
import com.pageon.backend.common.enums.OAuthProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdWithLock(Long userId);

    Boolean existsByEmail(String email);

    Boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u JOIN FETCH u.userRoles ur JOIN FETCH ur.role WHERE u.oAuthProvider = :oAuthProvider AND u.providerId = :providerId")
    Optional<User> findWithRolesByProviderAndProviderId(@Param("oAuthProvider") OAuthProvider oAuthProvider, @Param("providerId") String providerId);

    @Query("SELECT u FROM User u JOIN FETCH u.userRoles ur JOIN FETCH ur.role WHERE u.id = :userId")
    Optional<User> findWithRolesById(Long userId);

    @EntityGraph(attributePaths = {
            "userRoles", "userRoles.role"
    })
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Boolean existsByPhoneNumberAndIsPhoneVerifiedTrue(String phoneNumber);

    Boolean existsByIdAndIsPhoneVerifiedTrue(Long userId);

}
