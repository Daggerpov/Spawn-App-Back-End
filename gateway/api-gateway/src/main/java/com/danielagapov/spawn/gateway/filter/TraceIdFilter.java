package com.danielagapov.spawn.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter that propagates or generates a request trace ID for distributed tracing.
 * <p>
 * If the client sends {@code X-Trace-Id}, it is forwarded to downstream services and echoed
 * in the response. Otherwise a new UUID is generated. Downstream services can read
 * {@code X-Trace-Id} from the request and include it in their logs for correlation.
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public int getOrder() {
        // Run before JWT filter so trace ID is set for all requests including auth failures
        return -2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                org.springframework.http.HttpHeaders headers = super.getHeaders();
                if (!headers.containsKey(TRACE_ID_HEADER)) {
                    headers.add(TRACE_ID_HEADER, finalTraceId);
                }
                return headers;
            }
        };

        return chain.filter(exchange.mutate()
                .request(mutatedRequest)
                .response(responseDecorator)
                .build());
    }
}
