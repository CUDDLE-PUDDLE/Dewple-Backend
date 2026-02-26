package com.dewple.club.repository;

import com.dewple.club.entity.ClubDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubDepartmentRepository extends JpaRepository<ClubDepartment, Long> {

    Optional<ClubDepartment> findByIdAndClubId(Long id, Long clubId);
}
