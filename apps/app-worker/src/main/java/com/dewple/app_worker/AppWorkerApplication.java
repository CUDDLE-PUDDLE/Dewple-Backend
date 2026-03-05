package com.dewple.app_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dewple")
public class AppWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppWorkerApplication.class, args);
	}

}
