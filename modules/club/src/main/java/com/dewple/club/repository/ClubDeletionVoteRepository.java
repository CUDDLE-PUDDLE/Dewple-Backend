package com.dewple.club.repository;

import com.dewple.club.entity.ClubDeletionVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubDeletionVoteRepository extends JpaRepository<ClubDeletionVote, Long> {

    List<ClubDeletionVote> findByClubId(Long clubId);

    Optional<ClubDeletionVote> findByClubIdAndUserId(Long clubId, Long userId);

    void deleteByClubId(Long clubId);
}
