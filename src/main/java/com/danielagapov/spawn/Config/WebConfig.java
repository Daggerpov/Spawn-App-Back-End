package com.danielagapov.spawn.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TEMPORARY FIX FOR DEVELOPMENT: Allow all origins
        registry.addMapping("/api/v1/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false); // Must be false when using allowedOrigin("*")
                
        /* When you know the correct origin, replace the above with this:
        registry.addMapping("/api/v1/**")
                .allowedOrigins(
                    "https://spawn-app.vercel.app",
                    "http://localhost:3000",
                    // Add your simulator's origin here
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        */
    }
}