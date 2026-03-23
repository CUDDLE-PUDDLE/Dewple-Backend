package com.dewple.app_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.dewple")
@EnableScheduling
public class AppWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppWorkerApplication.class, args);
	}

}
