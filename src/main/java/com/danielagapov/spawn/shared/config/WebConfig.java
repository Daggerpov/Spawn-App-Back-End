package com.danielagapov.spawn.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS configuration for WebFlux (Reactor Netty with HTTP/3 support)
 * Replaced WebMvcConfigurer with reactive CORS filter for HTTP/3 compatibility
 */
@Configuration
public class WebConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList(
            "https://spawn-app.vercel.app",
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:4200",
            "http://localhost:8100", // ionic default
            "http://127.0.0.1:3000",
            "http://127.0.0.1:8080",
            "capacitor://localhost"
        ));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", corsConfig);

        return new CorsWebFilter(source);
    }
}