package com.danielagapov.spawn.shared.feign;

import feign.Logger;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign client configuration for inter-service HTTP calls.
 */
@Configuration
public class FeignConfig {

    /**
     * Log level for Feign calls. BASIC logs method, URL, response status, and execution time.
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Connection and read timeouts for Feign calls.
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS,   // connect timeout
                10, TimeUnit.SECONDS,  // read timeout
                true                   // follow redirects
        );
    }
}
