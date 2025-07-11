package com.danielagapov.spawn.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration to handle Hibernate lazy loading and other serialization issues
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Configure Hibernate module to handle lazy loading
        Hibernate6Module hibernateModule = new Hibernate6Module();
        
        // Configure how to handle lazy loading:
        // - FORCE_LAZY_LOADING: false means it won't try to load lazy properties
        // - USE_TRANSIENT_ANNOTATION: false means it won't use @Transient annotation for exclusion
        hibernateModule.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernateModule.configure(Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION, false);
        
        mapper.registerModule(hibernateModule);
        
        // Register Java Time module for proper date/time serialization
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (use ISO format instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Fail on empty beans (classes with no properties) to catch potential issues
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        return mapper;
    }
} 