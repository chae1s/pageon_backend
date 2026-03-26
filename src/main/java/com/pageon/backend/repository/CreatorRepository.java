package com.pageon.backend.repository;

import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
    Optional<Creator> findByUser(User users);

    Optional<Creator> findById(Long id);
}
