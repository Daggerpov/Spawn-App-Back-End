package com.danielagapov.spawn.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Centralised CORS configuration for the API Gateway.
 * <p>
 * All CORS handling happens at the gateway level so downstream services
 * do not need their own CORS config (they only accept internal traffic).
 */
@Configuration
public class CorsConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);

        if (isProduction) {
            config.setAllowedOrigins(List.of(
                    "https://getspawn.com",
                    "https://admin.getspawn.com",
                    "https://getspawn.com/admin"
            ));
        } else {
            config.setAllowedOrigins(List.of(
                    "https://getspawn.com",
                    "https://admin.getspawn.com",
                    "https://getspawn.com/admin",
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://localhost:8081",
                    "http://localhost:8090",
                    "http://localhost:4200",
                    "http://localhost:8100",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:8080",
                    "capacitor://localhost"
            ));
        }

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "X-Refresh-Token", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
