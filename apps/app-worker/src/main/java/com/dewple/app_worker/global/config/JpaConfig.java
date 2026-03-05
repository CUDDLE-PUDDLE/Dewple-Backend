package com.dewple.app_worker.global.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.dewple")
@EntityScan(basePackages = "com.dewple")
public class JpaConfig {
}
