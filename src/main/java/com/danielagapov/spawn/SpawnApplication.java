package com.danielagapov.spawn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class SpawnApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpawnApplication.class, args);
	}
}
