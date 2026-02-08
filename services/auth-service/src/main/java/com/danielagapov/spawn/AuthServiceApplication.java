package com.danielagapov.spawn;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
public class AuthServiceApplication {
    public static void main(String[] args) {

        // Skip environment variable loading for test profile
        String activeProfile = System.getProperty("spring.profiles.active",
                                                  System.getenv("SPRING_PROFILES_ACTIVE"));
        boolean isTestProfile = "test".equals(activeProfile);

        if (!isTestProfile) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            loadEnvVar(dotenv, "MYSQL_URL");
            loadEnvVar(dotenv, "MYSQL_USER");
            loadEnvVar(dotenv, "MYSQL_PASSWORD");
            loadEnvVar(dotenv, "EMAIL_PASS");
            loadEnvVar(dotenv, "GOOGLE_CLIENT_ID");
            loadEnvVar(dotenv, "APPLE_CLIENT_ID");
            loadEnvVar(dotenv, "SIGNING_SECRET");
        }

        SpringApplication.run(AuthServiceApplication.class, args);
    }

    private static void loadEnvVar(Dotenv dotenv, String key) {
        try {
            String value = System.getenv(key) != null ? System.getenv(key) : dotenv.get(key);
            if (value != null) {
                System.setProperty(key, value);
            }
        } catch (NullPointerException e) {
            System.err.println("Warning: " + key + " environment variable not set.");
        }
    }
}
