package com.dewple.club.repository;

import com.dewple.club.entity.ClubRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRegionRepository extends JpaRepository<ClubRegion, Long> {

    void deleteByClubId(Long clubId);
}
