package com.danielagapov.spawn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableJpaRepositories
public class SpawnApplication {
	public static void main(String[] args) {

		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

		System.setProperty("MYSQL_URL",
				System.getenv("MYSQL_URL") != null ? System.getenv("MYSQL_URL") : dotenv.get("MYSQL_URL"));
		System.setProperty("MYSQLUSER",
				System.getenv("MYSQLUSER") != null ? System.getenv("MYSQLUSER") : dotenv.get("MYSQLUSER"));
		System.setProperty("MYSQLPASSWORD",
				System.getenv("MYSQLPASSWORD") != null ? System.getenv("MYSQLPASSWORD") : dotenv.get("MYSQLPASSWORD"));

		SpringApplication.run(SpawnApplication.class, args);
	}
}
