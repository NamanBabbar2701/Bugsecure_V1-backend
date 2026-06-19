package com.bugsecure.backend;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class BugSecureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BugSecureBackendApplication.class, args);
	}

}
