package com.nowthink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NowthinkApplication {
	public static void main(String[] args) {
		SpringApplication.run(NowthinkApplication.class, args);
	}
}