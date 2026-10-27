package com.danielagapov.spawn.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;
    
    // Endpoints where a password or code can be guessed, so they get stricter limits.
    // OAuth sign-in isn't here: it runs on every app launch and its tokens can't be guessed.
    private final List<String> strictEndpoints = Arrays.asList(
        "/api/v1/auth/login",
        "/api/v1/auth/change-password",
        "/api/v1/auth/register/verification/check"
    );

    // Strict: 10 requests per 15 minutes
    private static final int STRICT_LIMIT = 10;
    private static final Duration STRICT_WINDOW = Duration.ofMinutes(15);

    // Normal: 60 requests per minute (onboarding alone makes several auth calls in a row)
    private static final int NORMAL_LIMIT = 60;
    private static final Duration NORMAL_WINDOW = Duration.ofMinutes(1);

    // In-memory cache for rate limiting, keyed by client IP and tier so the two windows don't mix
    private final Map<String, Deque<Instant>> requestCache = new ConcurrentHashMap<>();

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
        Instant now = Instant.now();
        int limit = isStrictEndpoint ? STRICT_LIMIT : NORMAL_LIMIT;
        Instant windowStart = now.minus(isStrictEndpoint ? STRICT_WINDOW : NORMAL_WINDOW);
        String key = clientIp + (isStrictEndpoint ? "|strict" : "|normal");

        Deque<Instant> requests = requestCache.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (requests) {
            // Remove requests older than the time window
            while (!requests.isEmpty() && !requests.peekFirst().isAfter(windowStart)) {
                requests.pollFirst();
            }
            if (requests.size() >= limit) {
                return true;
            }

            // Add current request
            requests.addLast(now);
            return false;
        }
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
