package com.dewple.app_api_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.dewple")
@EnableJpaRepositories(basePackages = "com.dewple")
@EntityScan(basePackages = "com.dewple")
public class AppApiAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApiAuthApplication.class, args);
	}

}
