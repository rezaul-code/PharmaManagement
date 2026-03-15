package com.myspringboot.SpringBootApp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringBootAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootAppApplication.class, args);
	}

}
