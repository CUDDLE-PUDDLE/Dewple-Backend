package com.dewple.user.repository;

import com.dewple.user.entity.Verification;
import com.dewple.user.repository.custom.VerificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationRepository extends JpaRepository<Verification, Long>, VerificationRepositoryCustom {

    Optional<Verification> findByToken(String token);

    Optional<Verification> findByPublicId(UUID publicId);
}
