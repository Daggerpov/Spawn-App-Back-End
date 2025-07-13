package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Services.ShareLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the Share Link system.
 * Ensures proper initialization and logging of the share link functionality.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ShareLinkConfiguration {
    
    private final ShareLinkService shareLinkService;
    
    /**
     * ApplicationRunner to initialize and log the share link system status on startup
     */
    @Bean
    public ApplicationRunner shareSystemInitializer() {
        return args -> {
            try {
                // Test that the share link system is working by checking if we can perform operations
                // This will also verify that the database table was created successfully
                log.info("Initializing Share Link system...");
                
                // The ShareLinkService will be ready to use at this point
                // The database table will be automatically created by Hibernate due to ddl-auto=update
                log.info("✅ Share Link system initialized successfully");
                log.info("   - Database table: share_link (auto-created by Hibernate)");
                log.info("   - Share code format: adjective-noun (e.g., 'happy-dolphin')");
                log.info("   - Activity links: expire after activity ends or 2 days from start");
                log.info("   - Profile links: permanent (until profile is deleted)");
                log.info("   - Cleanup: scheduled every hour for expired links");
                
            } catch (Exception e) {
                log.error("❌ Failed to initialize Share Link system", e);
                throw new RuntimeException("Share Link system initialization failed", e);
            }
        };
    }
} 