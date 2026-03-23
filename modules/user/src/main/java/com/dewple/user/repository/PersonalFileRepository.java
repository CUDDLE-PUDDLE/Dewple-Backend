package com.dewple.user.repository;

import com.dewple.common.enums.BaseStatus;
import com.dewple.user.entity.PersonalFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PersonalFileRepository extends JpaRepository<PersonalFile, Long> {

    List<PersonalFile> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BaseStatus status);

    Optional<PersonalFile> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM PersonalFile f WHERE f.user.id = :userId AND f.status = :status")
    long sumFileSizeByUserIdAndStatus(Long userId, BaseStatus status);
}
