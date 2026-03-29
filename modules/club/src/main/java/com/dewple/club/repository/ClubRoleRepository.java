package com.dewple.club.repository;

import com.dewple.club.entity.ClubRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubRoleRepository extends JpaRepository<ClubRole, Long> {

    Optional<ClubRole> findByClubIdAndName(Long clubId, String name);

    List<ClubRole> findByClubId(Long clubId);

    boolean existsByClubIdAndName(Long clubId, String name);
}
