package com.danielagapov.spawn.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;
    
    // Endpoints that require stricter rate limiting
    private final List<String> strictEndpoints = Arrays.asList(
        "/api/v1/auth/login",
        "/api/v1/auth/sign-in",
        "/api/v1/auth/change-password"
    );

    // In-memory cache for rate limiting
    private final Map<String, List<LocalDateTime>> requestCache = new ConcurrentHashMap<>();

    public RateLimitInterceptor(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIpAddress(request);
        String requestURI = request.getRequestURI();
        
        // Apply stricter limits to sensitive endpoints
        boolean isStrictEndpoint = strictEndpoints.stream().anyMatch(requestURI::contains);
        
        if (isRateLimited(clientIp, isStrictEndpoint)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Too many requests.\"}");
            return false;
        }
        
        return true;
    }

    private boolean isRateLimited(String clientIp, boolean isStrictEndpoint) {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean old entries and get current requests
        List<LocalDateTime> requests = requestCache.computeIfAbsent(clientIp, k -> new java.util.ArrayList<>());
        
        // Remove requests older than the time window
        if (isStrictEndpoint) {
            // Strict: 10 requests per hour
            requests.removeIf(time -> ChronoUnit.HOURS.between(time, now) >= 1);
            if (requests.size() >= 10) {
                return true;
            }
        } else {
            // Normal: 5 requests per minute
            requests.removeIf(time -> ChronoUnit.MINUTES.between(time, now) >= 1);
            if (requests.size() >= 5) {
                return true;
            }
        }
        
        // Add current request
        requests.add(now);
        return false;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null || xForwardedForHeader.isEmpty()) {
            return request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedForHeader.split(",")[0].trim();
        }
    }
} 