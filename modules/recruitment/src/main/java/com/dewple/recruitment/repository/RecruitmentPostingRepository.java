package com.dewple.recruitment.repository;

import com.dewple.recruitment.entity.RecruitmentPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentPostingRepository extends JpaRepository<RecruitmentPosting, Long> {

}

