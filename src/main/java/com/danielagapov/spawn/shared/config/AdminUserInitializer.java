package com.danielagapov.spawn.shared.config;

import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.UUID;

/**
 * This class is responsible for initializing the admin user when the application starts.
 * It creates an admin user if one doesn't already exist.
 */
@Configuration
public class AdminUserInitializer {

    @Value("${ADMIN_USERNAME:}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_PHONE_NUMBER:+1234567890}")
    private String adminPhoneNumber;

    @Bean
    public CommandLineRunner initializeAdminUser(
            IUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ILogger logger) {
        
        return args -> {
            // Validate admin credentials are properly configured
            if (adminUsername == null || adminUsername.trim().isEmpty()) {
                logger.warn("ADMIN_USERNAME environment variable not set. Admin user will not be created.");
                return;
            }
            
            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                logger.warn("ADMIN_PASSWORD environment variable not set. Admin user will not be created.");
                return;
            }
            
            // Validate password strength, but don't throw.
            if (!isStrongPassword(adminPassword)) {
                logger.warn("Admin password does not meet security requirements. Password must be at least 12 characters long and contain uppercase, lowercase, numbers, and special characters.");
            }

            // Check if admin user already exists
            if (!userRepository.existsByUsername(adminUsername)) {
                try {
                    logger.info("Creating admin user with username: " + adminUsername);
                    
                    // Create a new admin user
                    User adminUser = new User();
                    adminUser.setId(UUID.randomUUID());
                    adminUser.setUsername(adminUsername);
                    adminUser.setName("Admin User");
                    adminUser.setEmail("admin@getspawn.com");
                    adminUser.setPhoneNumber(adminPhoneNumber);
                    adminUser.setBio("Spawn Admin Account");
                    
                    // Encode the password from environment variable
                    adminUser.setPassword(passwordEncoder.encode(adminPassword));
                    
                    // Set as verified and created now
                    adminUser.setStatus(UserStatus.ACTIVE);
                    adminUser.setDateCreated(new Date());
                    
                    // Save to database
                    userRepository.save(adminUser);
                    
                    logger.info("Admin user created successfully with ID: " + adminUser.getId());
                } catch (Exception e) {
                    logger.error("Failed to create admin user: " + e.getMessage());
                    throw new RuntimeException("Failed to create admin user", e);
                }
            } else {
                logger.info("Admin user already exists with username: " + adminUsername);
            }
        };
    }
    
    @Value("${TEST_ACCOUNT_USERNAME:}")
    private String testAccountUsername;

    @Value("${TEST_ACCOUNT_PASSWORD:}")
    private String testAccountPassword;

    @Value("${TEST_ACCOUNT_NAME:Test User}")
    private String testAccountName;

    @Value("${TEST_ACCOUNT_EMAIL:}")
    private String testAccountEmail;

    /**
     * Seeds a password-login test account for a collaborator, like the admin
     * account App Store reviewers use. Credentials come from environment
     * variables so they never live in this (public) repo. Skipped when unset.
     */
    @Bean
    public CommandLineRunner initializeTestAccount(
            IUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ILogger logger) {

        return args -> {
            if (testAccountUsername == null || testAccountUsername.isBlank()
                    || testAccountPassword == null || testAccountPassword.isBlank()) {
                return;
            }

            if (userRepository.existsByUsername(testAccountUsername)) {
                logger.info("Test account already exists with username: " + testAccountUsername);
                return;
            }

            try {
                User testUser = new User();
                testUser.setId(UUID.randomUUID());
                testUser.setUsername(testAccountUsername);
                testUser.setName(testAccountName);
                if (testAccountEmail != null && !testAccountEmail.isBlank()) {
                    testUser.setEmail(testAccountEmail);
                }
                testUser.setBio("Spawn test account");
                testUser.setPassword(passwordEncoder.encode(testAccountPassword));
                testUser.setStatus(UserStatus.ACTIVE);
                testUser.setDateCreated(new Date());
                userRepository.save(testUser);

                logger.info("Test account created with username: " + testAccountUsername);
            } catch (Exception e) {
                // A broken test account must never stop the app from starting.
                logger.error("Failed to create test account: " + e.getMessage());
            }
        };
    }

    /**
     * Validates that the admin password meets security requirements
     */
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
} 