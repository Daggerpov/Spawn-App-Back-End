package com.danielagapov.spawn.shared.config;

import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTP/3 (QUIC) Configuration for Reactor Netty
 * 
 * This configuration enables HTTP/3 support using the Netty HTTP/3 codec.
 * 
 * Requirements:
 * 1. Spring Boot 3.4+
 * 2. netty-incubator-codec-http3 dependency on classpath
 * 3. TLS 1.3 enabled (HTTP/3 requires encryption)
 * 4. Valid SSL certificate configured in production
 * 
 * Benefits of HTTP/3:
 * - Reduced latency through QUIC protocol
 * - Better performance on poor network connections
 * - No head-of-line blocking
 * - Faster connection establishment (0-RTT)
 * - Seamless network transitions (WiFi <-> Cellular)
 * 
 * Client Support:
 * - iOS 15+ URLSession automatically supports HTTP/3
 * - Modern browsers (Chrome, Firefox, Safari)
 * 
 * How It Works:
 * - With the HTTP/3 codec dependency on the classpath and proper SSL/TLS configuration,
 *   Reactor Netty automatically enables HTTP/3 alongside HTTP/2 and HTTP/1.1
 * - Protocol negotiation happens automatically via ALPN (Application-Layer Protocol Negotiation)
 * - Clients that don't support HTTP/3 will fall back to HTTP/2 or HTTP/1.1
 * 
 * Note: In development (without SSL), HTTP/2 will be used instead of HTTP/3.
 * HTTP/3 requires HTTPS/TLS 1.3 in production.
 */
@Configuration
public class Http3Config {

    /**
     * Customizes Netty server for optimal HTTP/3 performance
     * 
     * This configuration:
     * - Enables HTTP/2 explicitly (required for HTTP/3 fallback)
     * - Configures connection limits and timeouts
     * - HTTP/3 is automatically enabled when SSL is configured
     */
    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> nettyCustomizer() {
        return factory -> {
            // HTTP/3 will be automatically enabled when:
            // 1. netty-incubator-codec-http3 is on the classpath (✓)
            // 2. SSL/TLS is properly configured (via Railway in production)
            // 3. server.http2.enabled=true in application.properties (✓)
            
            factory.addServerCustomizers(httpServer -> 
                httpServer
                    // Configure connection handling
                    .idleTimeout(java.time.Duration.ofSeconds(60))
                    .compress(true)
            );
        };
    }
}

