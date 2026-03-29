package com.dewple.club.repository;

import com.dewple.club.entity.ClubKickVote;
import com.dewple.club.entity.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClubKickVoteRepository extends JpaRepository<ClubKickVote, Long> {

    List<ClubKickVote> findByTargetMember(ClubMember targetMember);

    Optional<ClubKickVote> findByTargetMemberAndVoterId(ClubMember targetMember, Long voterId);

    void deleteByTargetMember(ClubMember targetMember);

    boolean existsByTargetMember(ClubMember targetMember);
}
