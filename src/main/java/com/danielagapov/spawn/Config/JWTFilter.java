package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.UserDetails.UserInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {
    private final IJWTService jwtService;
    private final ApplicationContext context;
    private final ILogger logger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.log("Executing JWT filter");
        String authHeader = request.getHeader("Authorization");

        // if no token is sent then skip jwt validation
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.log("Empty Authorization header, skipping JWT filter");
            filterChain.doFilter(request, response);
            return;
        }
        logger.log("Token found");
        String jwt = authHeader.substring(7);
        String username;
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            logger.log("Failed to extract username. Invalid or expired token");
            filterChain.doFilter(request, response);
            return;
        }

        // check if the user is not authenticated yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = context.getBean(UserInfoService.class).loadUserByUsername(username);
            if (jwtService.isValidToken(jwt, userDetails)) {
                logger.log("Token is valid, setting authentication");
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(token);
            } else {
                logger.log("Invalid token, user is not authenticated");
            }
        }
        filterChain.doFilter(request, response);
    }
}
