package com.dewple.recruitment.repository.custom;

import com.dewple.common.enums.ApplicationStatus;
import com.dewple.recruitment.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationRepositoryCustom {

    Page<Application> searchByPostingId(Long postingId, ApplicationStatus status, String keyword, Pageable pageable);
}
