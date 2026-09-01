package com.dashenbank.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ComplaintManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComplaintManagementSystemApplication.class, args);
	}

}
