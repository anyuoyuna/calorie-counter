package com.github.anyuoyuna.lifeassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LifeAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(LifeAssistantApplication.class, args);
	}

}
