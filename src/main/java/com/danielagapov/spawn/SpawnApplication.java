package com.danielagapov.spawn;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableJpaRepositories
@EnableScheduling
@EnableCaching
public class SpawnApplication {
    public static void main(String[] args) {
        
        // Skip environment variable loading for test profile
        String activeProfile = System.getProperty("spring.profiles.active", 
                                                  System.getenv("SPRING_PROFILES_ACTIVE"));
        // Also check if we're running in a test environment (e.g., via JUnit)
        boolean isTestProfile = "test".equals(activeProfile) || 
                               isRunningInTestEnvironment();
        
        if (!isTestProfile) {
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
            try {
                System.setProperty("APNS_CERTIFICATE",
                        System.getenv("APNS_CERTIFICATE") != null ? System.getenv("APNS_CERTIFICATE") : dotenv.get("APNS_CERTIFICATE"));
            } catch (NullPointerException e) {
                System.err.println("Error: APNS_CERTIFICATE environment variable not set. Consider setting, or adding a .env file.");
            }
            try {
                System.setProperty("CERTIFICATE_PASSWORD",
                        System.getenv("CERTIFICATE_PASSWORD") != null ? System.getenv("CERTIFICATE_PASSWORD") : dotenv.get("CERTIFICATE_PASSWORD"));
            } catch (NullPointerException e) {
                System.err.println("Error: CERTIFICATE_PASSWORD environment variable not set. Consider setting, or adding a .env file.");
            }
            try {
                System.setProperty("APNS_BUNDLE_ID",
                        System.getenv("APNS_BUNDLE_ID") != null ? System.getenv("APNS_BUNDLE_ID") : dotenv.get("APNS_BUNDLE_ID"));
            } catch (NullPointerException e) {
                System.err.println("Error: APNS_BUNDLE_ID environment variable not set. Consider setting, or adding a .env file.");
            }
            try {
                System.setProperty("GOOGLE_CLIENT_ID",
                        System.getenv("GOOGLE_CLIENT_ID") != null ? System.getenv("GOOGLE_CLIENT_ID") : dotenv.get("GOOGLE_CLIENT_ID"));
            } catch (NullPointerException e) {
                System.err.println("Error: GOOGLE_CLIENT_ID environment variable not set. Consider setting it in your environment or .env file.");
                System.err.println("Google authentication will not work without this value.");
            }
            try {
                System.setProperty("APPLE_CLIENT_ID",
                        System.getenv("APPLE_CLIENT_ID") != null ? System.getenv("APPLE_CLIENT_ID") : dotenv.get("APPLE_CLIENT_ID"));
            } catch (NullPointerException e) {
                System.err.println("Error: APPLE_CLIENT_ID environment variable not set. Consider setting it in your environment or .env file.");
                System.err.println("Apple authentication will not work without this value.");
            }
        }
        
        SpringApplication.run(SpawnApplication.class, args);
    }

    /**
     * Checks if the application is running in a test environment.
     * This method looks for common test indicators like JUnit classes on the classpath.
     */
    private static boolean isRunningInTestEnvironment() {
        try {
            // Check if JUnit is on the classpath
            Class.forName("org.junit.jupiter.api.Test");
            
            // Check for test-specific system properties
            String testProperty = System.getProperty("java.class.path");
            if (testProperty != null && testProperty.contains("test-classes")) {
                return true;
            }
            
            // Check if running from a test runner
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (className.contains("junit") || className.contains("Test")) {
                    return true;
                }
            }
            
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
