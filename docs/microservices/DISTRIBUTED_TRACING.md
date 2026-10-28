# Distributed Tracing (X-Trace-Id)

Request tracing is implemented so logs across the gateway and backend services can be correlated by a single ID.

## How it works

1. **API Gateway** (`gateway/api-gateway`):
   - **TraceIdFilter** runs first on every request.
   - If the client sends an `X-Trace-Id` header, it is reused; otherwise a new UUID is generated.
   - The chosen trace ID is added to the request forwarded to downstream services and echoed in the response (`X-Trace-Id`).

2. **Monolith** (`src/main/java`):
   - **TraceIdMdcFilter** reads `X-Trace-Id` from the request (or generates one for direct calls) and puts it in SLF4J MDC under the key `traceId`.
   - Logback is configured so log lines include `traceId=...`. This allows log aggregation tools to correlate all log entries for a given request.

3. **Other services** (auth-service, activity-service):
   - When called via the gateway, they receive `X-Trace-Id` on the request.
   - To include trace ID in their logs, add a similar filter that reads `X-Trace-Id` and puts it in MDC, and add `traceId=%X{traceId:-}` (or equivalent) to the log pattern.

## Client usage

Clients can send an `X-Trace-Id` header (e.g. a UUID they generate) when making requests. The same value will be returned in the response and used for all downstream calls, making it easy to correlate client-side logs with backend logs.

## Feign / outbound calls

When a service (e.g. activity-service) calls another service via Feign, the gateway is not in the path. To propagate the trace ID:

- Add a Feign `RequestInterceptor` that reads the current MDC `traceId` (or request attribute) and sets the `X-Trace-Id` header on the outgoing request.
- Ensure the receiving service has a filter that puts `X-Trace-Id` into MDC (as in the monolith).
