package com.danielagapov.spawn.shared.config;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final JWTFilterConfig jwtFilterConfig;
    private final String[] whitelistedUrls = new String[] {
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/register/verification/send",
            "/api/v1/auth/register/oauth",
            "/api/v1/auth/register/verification/check",
            "/api/v1/auth/sign-in",
            "/api/v1/auth/login",
            "/api/v1/users/contacts/cross-reference"
    };
    // Additional regex patterns for whitelisted URLs
    private final String[] whitelistedUrlPatterns = new String[] {
            "/api/v1/auth/accept-tos/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            "/api/v1/auth/complete-contact-import/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
            "/api/v1/users/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/optional-details"
    };

    private static final String[] onboardingUrls = new String[] {
            "/api/v1/auth/register/verification/check",
            "/api/v1/auth/user/details",
            "/api/v1/auth/accept-tos/**",
            "/api/v1/auth/complete-contact-import/**",
            "/api/v1/users/*/optional-details"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOrigins(List.of(
                            "https://getspawn.com",
                            "https://getspawn.com/admin",
                            "http://localhost:3000",
                            "http://localhost:8080",
                            "http://localhost:4200",
                            "http://localhost:8100", // ionic default
                            "http://127.0.0.1:3000",
                            "http://127.0.0.1:8080",
                            "capacitor://localhost"
                    ));

                    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    configuration.setAllowedHeaders(List.of("Authorization", "X-Refresh-Token", "Content-Type", "Accept"));
                    configuration.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
                    configuration.setAllowCredentials(true);
                    return configuration;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                // Endpoints can be made unsecured by specifying it with requestMatchers() below and permitting
                // that be accessed without authentication with permitAll().
                // Below, the auth and oauth endpoints are unsecured
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(whitelistedUrls).permitAll();
                    authorize.requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.GET, "/api/v1/activities/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(\\?.*)?")).permitAll(); // Allow GET requests to specific activity by UUID for external invites (with optional query parameters)
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/betaAccessSignUp").permitAll(); // Allow POST requests for beta access signup
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/share/activity/**").permitAll(); // Allow GET requests to resolve activity share codes
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/share/profile/**").permitAll(); // Allow GET requests to resolve profile share codes
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/share/validate/**").permitAll(); // Allow GET requests to validate share codes
                    
                    // Add whitelisted URL patterns
                    for (String pattern : whitelistedUrlPatterns) {
                        authorize.requestMatchers(RegexRequestMatcher.regexMatcher(HttpMethod.POST, pattern)).permitAll();
                    }

                    for (String pattern : onboardingUrls) {
                        authorize.requestMatchers(pattern).hasRole("ONBOARDING");
                    }

                    authorize.requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN","ONBOARDING","ACTIVE");
                    authorize.requestMatchers("/api/v1/feedback/**").hasAnyRole("ADMIN","ONBOARDING","ACTIVE");
                    authorize.requestMatchers("/api/v1/betaAccessSignUp/**").hasAnyRole("ADMIN","ONBOARDING","ACTIVE");
                    
                    authorize.requestMatchers("/api/v1/auth/quick-sign-in").hasAnyRole("ONBOARDING","ACTIVE");
                    authorize.requestMatchers("/api/v1/**").hasRole("ACTIVE");
                    authorize.anyRequest().authenticated(); // Comment this out if wanting to unsecure endpoints for development purposes
                })
                // When authenticating a request fails, status code 401 (unauthorized) is returned
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // 'Stateless' session management means Spring will not create and store any session state on the server
                // Each request is treated as 'new' and thus requires authentication (a JWT) to access secured endpoints
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilterConfig, UsernamePasswordAuthenticationFilter.class)
        ;
        return http.build();
    }


    /**
     * Provides the default AuthenticationManager bean.
     * <p>
     * The AuthenticationManager is responsible for processing authentication requests.
     * It delegates authentication to the configured AuthenticationProvider.
     *
     * @param config The AuthenticationConfiguration provided by Spring Security.
     * @return The AuthenticationManager instance.
     * @throws Exception If an error occurs while retrieving the authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager(); // use default auth manager
    }


    /**
     * Provides a password encoder bean.
     * <p>
     * BCryptPasswordEncoder is a strong and secure password hashing algorithm
     * that automatically manages salting for added security.
     *
     * @return An instance of BCryptPasswordEncoder.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
