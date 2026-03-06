package com.danielagapov.spawn;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@EnableFeignClients
public class ChatServiceApplication {

    public static void main(String[] args) {
        String activeProfile = System.getProperty("spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
        boolean isTestProfile = "test".equals(activeProfile);

        if (!isTestProfile) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            loadEnvVar(dotenv, "MYSQL_URL");
            loadEnvVar(dotenv, "MYSQL_USER");
            loadEnvVar(dotenv, "MYSQL_PASSWORD");
            loadEnvVar(dotenv, "REDIS_HOST");
            loadEnvVar(dotenv, "REDIS_PORT");
            loadEnvVar(dotenv, "REDIS_PASSWORD");
            loadEnvVar(dotenv, "MONOLITH_URL");
        }

        SpringApplication.run(ChatServiceApplication.class, args);
    }

    private static void loadEnvVar(Dotenv dotenv, String key) {
        try {
            String value = System.getenv(key) != null ? System.getenv(key) : dotenv.get(key);
            if (value != null) {
                System.setProperty(key, value);
            }
        } catch (NullPointerException ignored) {
        }
    }
}
