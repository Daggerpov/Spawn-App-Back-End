package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Services.UserDetails.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final UserInfoService userInfoService;
    private final JWTFilterConfig jwtFilterConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOrigins(List.of(
                            "https://getspawn.com",
                            "https://admin.getspawn.com",
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
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/**", "/api/v1/notifications/notification").permitAll()
                        .anyRequest()
                        .authenticated() // Comment this out if wanting to unsecure endpoints for development purposes
                )
                // When authenticating a request fails, status code 401 (unauthorized) is returned
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                // 'Stateless' session management means Spring will not create and store any session state on the server
                // Each request is treated as 'new' and thus requires authentication (a JWT) to access secured endpoints
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
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
     * Configures the authentication provider for the application.
     * <p>
     * DaoAuthenticationProvider is used to authenticate users by retrieving user details
     * from the database and validating their credentials.
     *
     * @return A configured DaoAuthenticationProvider instance.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userInfoService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
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

    /**
     * Provides a UserDetailsService bean.
     * <p>
     * This service is responsible for loading user details from the database
     * based on the username during authentication.
     *
     * @return An instance of UserDetailsService, implemented by UserInfoService.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return userInfoService;
    }
}
