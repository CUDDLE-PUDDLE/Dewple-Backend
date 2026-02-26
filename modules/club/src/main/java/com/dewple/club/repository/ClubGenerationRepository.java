package com.dewple.club.repository;

import com.dewple.club.entity.ClubGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubGenerationRepository extends JpaRepository<ClubGeneration, Long> {

    Optional<ClubGeneration> findByClubIdAndGenerationNo(Long clubId, Integer generationNo);
}
