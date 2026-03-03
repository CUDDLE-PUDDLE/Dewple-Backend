package com.dewple.recruitment.repository;

import com.dewple.recruitment.entity.Application;
import com.dewple.recruitment.repository.custom.ApplicationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, ApplicationRepositoryCustom {
}
