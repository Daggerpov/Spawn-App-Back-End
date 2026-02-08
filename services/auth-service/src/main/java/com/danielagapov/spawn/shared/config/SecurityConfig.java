package com.danielagapov.spawn.shared.config;

import com.danielagapov.spawn.user.internal.services.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Security configuration for the auth microservice.
 * Simplified to only handle auth-related endpoints.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final JWTFilterConfig jwtFilterConfig;
    private final UserInfoService userInfoService;
    
    private final String[] whitelistedUrls = new String[] {
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/register/verification/send",
            "/api/v1/auth/register/oauth",
            "/api/v1/auth/register/verification/check",
            "/api/v1/auth/sign-in",
            "/api/v1/auth/login",
    };

    private final String[] whitelistedUrlPatterns = new String[] {
            "/api/v1/auth/accept-tos/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            "/api/v1/auth/complete-contact-import/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
    };

    private static final String[] onboardingUrls = new String[] {
            "/api/v1/auth/register/verification/check",
            "/api/v1/auth/user/details",
            "/api/v1/auth/accept-tos/**",
            "/api/v1/auth/complete-contact-import/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    
                    String environment = System.getProperty("spring.profiles.active", "dev");
                    boolean isProduction = "prod".equals(environment) || "production".equals(environment);
                    
                    if (isProduction) {
                        configuration.setAllowedOrigins(List.of(
                                "https://getspawn.com",
                                "https://admin.getspawn.com",
                                "https://getspawn.com/admin"
                        ));
                    } else {
                        configuration.setAllowedOrigins(List.of(
                                "https://getspawn.com",
                                "https://admin.getspawn.com",
                                "https://getspawn.com/admin",
                                "http://localhost:3000",
                                "http://localhost:8080",
                                "http://localhost:8081",
                                "http://localhost:4200",
                                "http://localhost:8100",
                                "http://127.0.0.1:3000",
                                "http://127.0.0.1:8080",
                                "capacitor://localhost"
                        ));
                    }

                    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    configuration.setAllowedHeaders(List.of("Authorization", "X-Refresh-Token", "Content-Type", "Accept"));
                    configuration.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
                    configuration.setAllowCredentials(true);
                    configuration.setMaxAge(3600L);
                    return configuration;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(contentType -> {})
                )
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(whitelistedUrls).permitAll();
                    
                    for (String pattern : whitelistedUrlPatterns) {
                        authorize.requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.POST, pattern)).permitAll();
                    }

                    for (String pattern : onboardingUrls) {
                        authorize.requestMatchers(pattern).hasRole("ONBOARDING");
                    }

                    authorize.requestMatchers("/api/v1/auth/quick-sign-in").hasAnyRole("ONBOARDING","ACTIVE");
                    authorize.requestMatchers("/api/v1/auth/**").hasAnyRole("ACTIVE", "ONBOARDING");
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            String clientIp = getClientIpAddress(request);
                            String requestUrl = request.getRequestURL().toString();
                            
                            System.out.println("Authentication failed - IP: " + clientIp + 
                                ", URL: " + requestUrl + 
                                ", Error: " + authException.getMessage());
                            
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Authentication required\"}");
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilterConfig, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userInfoService;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isEmpty()) {
            return request.getRemoteAddr();
        } else {
            return xForwardedForHeader.split(",")[0].trim();
        }
    }
}
