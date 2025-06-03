package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.UUID;

/**
 * This class is responsible for initializing the admin user when the application starts.
 * It creates an admin user if one doesn't already exist.
 */
@Configuration
@Profile("!test") // Exclude from test profile
public class AdminUserInitializer {

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:spawn-admin-secure-password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner initializeAdminUser(
            IUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ILogger logger) {
        
        return args -> {
            // Check if admin user already exists
            if (!userRepository.existsByUsername(adminUsername)) {
                try {
                    logger.info("Creating admin user");
                    
                    // Create a new admin user
                    User adminUser = new User();
                    adminUser.setId(UUID.randomUUID());
                    adminUser.setUsername(adminUsername);
                    adminUser.setName("Admin User");
                    adminUser.setEmail("admin@getspawn.com");
                    adminUser.setBio("Spawn Admin Account");
                    
                    // Encode the password from environment variable
                    adminUser.setPassword(passwordEncoder.encode(adminPassword));
                    
                    // Set as verified and created now
                    adminUser.setVerified(true);
                    adminUser.setDateCreated(new Date());
                    
                    // Save to database
                    userRepository.save(adminUser);
                    
                    logger.info("Admin user created successfully");
                } catch (Exception e) {
                    logger.error("Failed to create admin user: " + e.getMessage());
                }
            } 
        };
    }
} 