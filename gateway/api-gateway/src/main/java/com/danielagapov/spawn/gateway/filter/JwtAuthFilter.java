package com.danielagapov.spawn.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Global JWT authentication filter for the API Gateway.
 * <p>
 * Validates JWT tokens on incoming requests and adds an X-User-Id header
 * (containing the token subject) to forwarded requests so downstream services
 * can identify the authenticated user without re-validating the token.
 * <p>
 * Requests to whitelisted paths (e.g. auth endpoints) bypass validation.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /**
     * Paths that do not require JWT authentication.
     * Auth endpoints are open so users can sign in / register / refresh tokens.
     * Actuator health is open for load-balancer probes.
     */
    private static final List<String> OPEN_PATHS = List.of(
            "/api/v1/auth/sign-in",
            "/api/v1/auth/login",
            "/api/v1/auth/register/verification/send",
            "/api/v1/auth/register/oauth",
            "/api/v1/auth/register/verification/check",
            "/api/v1/auth/refresh-token",
            "/actuator/health"
    );

    /**
     * Path prefixes that are open without JWT validation.
     * These cover endpoints with dynamic path segments (e.g. UUIDs).
     */
    private static final List<String> OPEN_PATH_PREFIXES = List.of(
            "/api/v1/auth/accept-tos/",
            "/api/v1/auth/complete-contact-import/",
            // Public share-link and beta-access endpoints from the monolith
            "/api/v1/share-links/",
            "/api/v1/beta-access/"
    );

    private final String signingSecret;

    public JwtAuthFilter(@Value("${jwt.signing-secret:#{null}}") String configuredSecret) {
        // Priority: 1) Spring property / env, 2) SIGNING_SECRET env var, 3) .env file
        this.signingSecret = resolveSigningSecret(configuredSecret);
    }

    private static String resolveSigningSecret(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isEmpty()) {
            return configuredSecret;
        }
        String envSecret = System.getenv("SIGNING_SECRET");
        if (envSecret != null && !envSecret.isEmpty()) {
            return envSecret;
        }
        try {
            var dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
            return dotenv.get("SIGNING_SECRET");
        } catch (Exception e) {
            log.warn("Could not load .env file: {}", e.getMessage());
            return null;
        }
    }

    @PostConstruct
    void validate() {
        if (signingSecret == null || signingSecret.isBlank()) {
            log.error("JWT signing secret is not configured! Set jwt.signing-secret or SIGNING_SECRET env var.");
        } else {
            log.info("JWT signing secret loaded successfully.");
        }
    }

    @Override
    public int getOrder() {
        // Run early so auth is checked before any other filters
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip JWT validation for open/whitelisted paths
        if (isOpenPath(path)) {
            return chain.filter(exchange);
        }

        // Extract Bearer token from Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // Validate the token
        Claims claims;
        try {
            claims = extractAllClaims(token);
        } catch (ExpiredJwtException e) {
            return onUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            return onUnauthorized(exchange, "Invalid token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error validating JWT: {}", e.getMessage());
            return onUnauthorized(exchange, "Token validation failed");
        }

        // Validate issuer and audience
        if (!"spawn-backend".equals(claims.getIssuer())) {
            return onUnauthorized(exchange, "Invalid token issuer");
        }
        Set<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains("spawn-app")) {
            return onUnauthorized(exchange, "Invalid token audience");
        }

        // Validate token type is ACCESS
        String tokenType = (String) claims.get("type");
        if (!"ACCESS".equals(tokenType)) {
            return onUnauthorized(exchange, "Token is not an access token");
        }

        // Token is valid -- inject X-User-Id header for downstream services
        String subject = claims.getSubject();
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", subject)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /* ----------------------------- Helpers ----------------------------- */

    private boolean isOpenPath(String path) {
        for (String open : OPEN_PATHS) {
            if (path.equals(open)) {
                return true;
            }
        }
        for (String prefix : OPEN_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(signingSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Mono<Void> onUnauthorized(ServerWebExchange exchange, String reason) {
        log.warn("JWT auth failed for {} {}: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                reason);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"error\":\"Authentication required\",\"detail\":\"" + reason + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
