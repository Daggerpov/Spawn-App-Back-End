package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
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

    @Bean
    public CommandLineRunner initializeAdminUser(
            IUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ILogger logger) {
        
        return args -> {
            // Check if admin user already exists
            if (!userRepository.existsByUsername("admin")) {
                try {
                    logger.info("Creating admin user");
                    
                    // Create a new admin user
                    User adminUser = new User();
                    adminUser.setId(UUID.randomUUID());
                    adminUser.setUsername("admin");
                    adminUser.setFirstName("Admin");
                    adminUser.setLastName("User");
                    adminUser.setEmail("admin@getspawn.com");
                    adminUser.setBio("Spawn Admin Account");
                    
                    // Encode the password for security
                    adminUser.setPassword(passwordEncoder.encode("spawn-admin-2024"));
                    
                    // Set as verified and created now
                    adminUser.setVerified(true);
                    adminUser.setDateCreated(new Date());
                    
                    // Save to database
                    userRepository.save(adminUser);
                    
                    logger.info("Admin user created successfully");
                } catch (Exception e) {
                    logger.error("Failed to create admin user: " + e.getMessage());
                }
            } else {
                logger.info("Admin user already exists");
            }
        };
    }
} 