package com.dewple.recruitment.repository;

import com.dewple.common.enums.ProcessType;
import com.dewple.recruitment.entity.RecruitmentSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruitmentSchemaRepository extends JpaRepository<RecruitmentSchema, Long> {

    @Query("SELECT rs FROM RecruitmentSchema rs " +
            "WHERE rs.recruitmentProcess.posting.id = :postingId " +
            "AND rs.recruitmentProcess.processType = :processType " +
            "ORDER BY rs.version DESC " +
            "LIMIT 1")
    Optional<RecruitmentSchema> findLatestByPostingIdAndProcessType(
            @Param("postingId") Long postingId,
            @Param("processType") ProcessType processType);
}
