package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.UserDetails.UserInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JWTFilterConfig extends OncePerRequestFilter {
    private final IJWTService jwtService;
    private final ApplicationContext context;
    private final ILogger logger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // Retrieve the Authorization header from the HTTP request
            String authHeader = request.getHeader("Authorization");

            // Check if the Authorization header is missing or does not start with "Bearer "
            // If so, skip JWT validation and proceed with the next filter in the chain
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            // Extract the JWT token from the Authorization header (removing the "Bearer " prefix)
            String jwt = authHeader.substring(7);
            String username;
            try {
                username = jwtService.extractUsername(jwt);
            } catch (Exception e) {
                logger.warn("Failed to extract username. Invalid or expired token: " + e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }

            // Check if the username was successfully extracted and if the user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // Load the UserDetails object for the extracted username using the UserInfoService
                    UserDetails userDetails = context.getBean(UserInfoService.class).loadUserByUsername(username);

                    // Validate the JWT token against the UserDetails
                    if (jwtService.isValidToken(jwt, userDetails)) {
                        /*
                         * Create an authentication token containing the user details and authorities.
                         * UsernamePasswordAuthenticationToken is a Spring Security authentication object
                         * that represents a successfully authenticated user.
                         * WebAuthenticationDetailsSource is used to build additional details about the authentication request
                         * This includes information like the remote IP address
                         */
                        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        /*
                         * Set the authentication in SecurityContextHolder so that Spring Security
                         * recognizes the user as authenticated for the current request.
                         */
                        SecurityContextHolder.getContext().setAuthentication(token);
                    } else {
                        logger.warn("Invalid token, user is not authenticated");
                    }
                } catch (UsernameNotFoundException e) {
                    logger.warn("User not found: " + username);
                } catch (Exception e) {
                    logger.error("Error during authentication: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Unexpected error in JWT filter: " + e.getMessage());
            // Continue with filter chain even if JWT processing fails
        }
        
        // Proceed with the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
