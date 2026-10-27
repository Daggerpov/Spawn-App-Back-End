package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.shared.config.RateLimitConfig;
import com.danielagapov.spawn.shared.config.RateLimitInterceptor;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RateLimitInterceptor - per-IP limits on auth endpoints.
 */
@DisplayName("Rate Limit Interceptor Tests")
class RateLimitInterceptorTests {

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(new RateLimitConfig());
    }

    private int send(String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, new Object());
        return allowed ? 200 : response.getStatus();
    }

    @Test
    @DisplayName("Onboarding burst on normal endpoints is allowed up to 60 per minute")
    void normalEndpointsAllowSixtyPerMinute() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertThat(send("/api/v1/auth/accept-tos/123", "1.1.1.1")).isEqualTo(200);
        }
        assertThat(send("/api/v1/auth/accept-tos/123", "1.1.1.1")).isEqualTo(429);
    }

    @Test
    @DisplayName("Strict endpoints are limited to 10 and don't consume the normal budget")
    void strictEndpointsHaveTheirOwnBudget() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(send("/api/v1/auth/login", "2.2.2.2")).isEqualTo(200);
        }
        assertThat(send("/api/v1/auth/login", "2.2.2.2")).isEqualTo(429);
        assertThat(send("/api/v1/auth/user/details", "2.2.2.2")).isEqualTo(200);
    }

    @Test
    @DisplayName("OAuth sign-in uses the normal limit")
    void oauthSignInIsNotStrict() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(send("/api/v1/auth/sign-in", "3.3.3.3")).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("Limits are per client IP")
    void limitsArePerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            send("/api/v1/auth/login", "4.4.4.4");
        }
        assertThat(send("/api/v1/auth/login", "4.4.4.4")).isEqualTo(429);
        assertThat(send("/api/v1/auth/login", "5.5.5.5")).isEqualTo(200);
    }
}
