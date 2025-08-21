package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
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
            
            // Validate password strength
            if (!isStrongPassword(adminPassword)) {
                logger.error("Admin password does not meet security requirements. Password must be at least 12 characters long and contain uppercase, lowercase, numbers, and special characters.");
                throw new SecurityException("Admin password does not meet security requirements");
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