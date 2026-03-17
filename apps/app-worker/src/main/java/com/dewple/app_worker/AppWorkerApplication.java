package com.dewple.app_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"com.dewple.app_worker",
		"com.dewple.common.config"
})
public class AppWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppWorkerApplication.class, args);
	}

}
