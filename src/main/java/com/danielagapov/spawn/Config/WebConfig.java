package com.danielagapov.spawn.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(
                    "https://spawn-app.vercel.app",
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://localhost:4200",
                    "http://localhost:8100", // ionic default
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:8080",
                    "capacitor://localhost"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}