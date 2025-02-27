package com.danielagapov.spawn;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
public class SpawnApplication {
    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        try {
            System.setProperty("MYSQL_URL",
                    System.getenv("MYSQL_URL") != null ? System.getenv("MYSQL_URL") : dotenv.get("MYSQL_URL"));
        } catch (NullPointerException e) {
            System.err.println("Error: MYSQL_URL environment variable not set. Consider setting, or adding a .env file.");
        }
        try {
            System.setProperty("MYSQL_USER",
                    System.getenv("MYSQL_USER") != null ? System.getenv("MYSQL_USER") : dotenv.get("MYSQL_USER"));
        } catch (NullPointerException e) {
            System.err.println("Error: MYSQL_USER environment variable not set. Consider setting, or adding a .env file.");
        }
        try {
            System.setProperty("MYSQL_PASSWORD",
                    System.getenv("MYSQL_PASSWORD") != null ? System.getenv("MYSQL_PASSWORD") : dotenv.get("MYSQL_PASSWORD"));
        } catch (NullPointerException e) {
            System.err.println("Error: MYSQL_PASSWORD environment variable not set. Consider setting, or adding a .env file.");
        }
        try {
            System.setProperty("EMAIL_PASS",
                    System.getenv("EMAIL_PASS") != null ? System.getenv("EMAIL_PASS") : dotenv.get("EMAIL_PASS"));
        } catch (NullPointerException e) {
            System.err.println("Error: EMAIL_PASS environment variable not set. Consider setting, or adding a .env file.");
        }
        SpringApplication.run(SpawnApplication.class, args);
    }
}
