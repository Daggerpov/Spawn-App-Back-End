# HTTP/3 (QUIC) Implementation Guide

## Overview

This document describes the HTTP/3 implementation for Spawn App, enabling faster, more reliable connections between the iOS app and back-end server using the QUIC protocol.

## What is HTTP/3?

HTTP/3 is the latest version of HTTP, built on the QUIC transport protocol instead of TCP. Key benefits:

- **Reduced Latency**: Faster connection establishment (0-RTT)
- **No Head-of-Line Blocking**: Individual streams don't block each other
- **Better Performance on Poor Networks**: Handles packet loss more gracefully
- **Seamless Network Transitions**: Maintains connections when switching between WiFi and cellular
- **Built-in Encryption**: QUIC requires TLS 1.3 by design

## Implementation Details

### Back-End Changes

#### 1. Spring Boot Upgrade
- **Upgraded**: Spring Boot 3.3.5 → 3.4.1
- **Reason**: Spring Boot 3.4+ includes Reactor Netty 1.3 with HTTP/3 support

#### 2. Server Switch
- **Changed**: `spring-boot-starter-web` (Tomcat) → `spring-boot-starter-webflux` (Reactor Netty)
- **Impact**: Now using reactive programming model
- **Compatibility**: Most controllers work without changes; blocking operations may need adjustment

#### 3. Dependencies Added

```xml
<!-- Reactor Netty with HTTP/3 support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Netty HTTP/3 codec -->
<dependency>
    <groupId>io.netty.incubator</groupId>
    <artifactId>netty-incubator-codec-http3</artifactId>
    <version>0.0.28.Final</version>
    <scope>runtime</scope>
</dependency>

<!-- Native transport for better performance -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <classifier>linux-x86_64</classifier>
</dependency>
```

#### 4. Configuration Files

**Http3Config.java**: Configures Reactor Netty to support HTTP/1.1, HTTP/2, and HTTP/3 simultaneously
**WebConfig.java**: Updated from Spring MVC CORS to WebFlux reactive CORS filter
**application.properties**: Added HTTP/3 and Reactor Netty configuration

### iOS App

**No changes required!** 

- iOS 15+ URLSession automatically supports HTTP/3
- The app already uses `URLSession.shared`, which handles protocol negotiation
- Will automatically use HTTP/3 when the server supports it
- Falls back to HTTP/2 or HTTP/1.1 if HTTP/3 is unavailable

### Protocol Negotiation

The implementation supports **graceful fallback**:

1. **Best Case**: Client + Server support HTTP/3 → Uses HTTP/3 (QUIC)
2. **Fallback 1**: Server supports HTTP/3, client doesn't → Uses HTTP/2
3. **Fallback 2**: Neither supports HTTP/3 → Uses HTTP/1.1

## Railway Deployment

### Current Status

Railway **does not explicitly document HTTP/3 support** at the infrastructure level. However, there are two deployment options:

### Option 1: Standard Deployment (Current)

Deploy as normal. The app will:
- ✅ Support HTTP/2 (if Railway enables it at the edge)
- ✅ Support HTTP/1.1 (guaranteed)
- ❓ HTTP/3 support depends on Railway's infrastructure

**Action**: Deploy and test to see if Railway supports HTTP/3 passthrough.

### Option 2: Deploy with rpxy Reverse Proxy (Recommended for Full HTTP/3)

Railway offers an [rpxy proxy template](https://railway.com/deploy/rpxy-proxy) that explicitly supports HTTP/3.

#### Setup Steps:

1. **Deploy rpxy proxy on Railway**:
   - Click the Railway rpxy template link
   - Deploy to your Railway project
   - Configure it to proxy to your Spring Boot app

2. **Update DNS**:
   - Point your domain to the rpxy proxy
   - Configure rpxy to forward to your Spring Boot service

3. **Configure SSL/TLS**:
   - Add your SSL certificate to rpxy
   - Ensure TLS 1.3 is enabled (required for HTTP/3)

4. **Update iOS App**:
   - Change `ServiceConstants.URLs.apiBase` to point to rpxy proxy URL
   - No other iOS changes needed

#### rpxy Configuration Example:

```toml
# rpxy.toml
[[apps]]
server_name = "spawn-app-back-end-production.up.railway.app"
reverse_proxy = [
  { upstream = [{ location = "your-spring-boot-service:8080" }] }
]

[apps.tls]
https_redirection = true
tls_versions = ["1.3"]

[apps.http3]
enabled = true
```

## Testing HTTP/3

### Using curl

```bash
# Test if server supports HTTP/3
curl --http3 -I https://spawn-app-back-end-production.up.railway.app/api/v1/health

# If HTTP/3 is working, you'll see:
# HTTP/3 200
```

### Using Chrome DevTools

1. Open Chrome DevTools → Network tab
2. Add "Protocol" column (right-click headers)
3. Make requests to your API
4. Look for "h3" in the Protocol column (h3 = HTTP/3)

### Using iOS App

1. Deploy updated back-end to Railway
2. Run iOS app on iOS 15+ device
3. Use Network Link Conditioner or Charles Proxy to verify protocol
4. Check Xcode console for connection details

## Performance Improvements

Expected improvements with HTTP/3:

- **Connection Establishment**: ~50-100ms faster (0-RTT vs TCP handshake)
- **Poor Networks**: 10-30% better throughput on lossy connections
- **Network Transitions**: Seamless WiFi ↔ Cellular handoff
- **API Calls**: Reduced latency, especially for multiple parallel requests

## Monitoring

Monitor these metrics after deployment:

- **Connection Time**: Should decrease with HTTP/3
- **Request Latency**: Especially for concurrent requests
- **Error Rates**: Should remain stable or improve
- **Network Transition Handling**: Better resilience during network changes

## Rollback Plan

If issues occur, rollback is simple:

1. **Revert pom.xml**: Change back to Spring Boot 3.3.5 and `spring-boot-starter-web`
2. **Revert WebConfig.java**: Change back to `WebMvcConfigurer`
3. **Remove**: Http3Config.java and HTTP/3 dependencies
4. **Restore**: Tomcat configuration in application.properties
5. **Redeploy**: Standard deployment

## Known Limitations

1. **Railway HTTP/3**: Not officially documented; may require rpxy proxy
2. **TLS 1.3 Required**: Ensure SSL certificates support TLS 1.3
3. **Client Support**: Requires iOS 15+ (95%+ of Spawn users)
4. **UDP Ports**: HTTP/3 uses UDP; ensure Railway allows UDP traffic on port 443

## Next Steps

1. ✅ Code changes complete
2. 🔄 Deploy to Railway
3. 🔄 Test HTTP/3 connectivity
4. 🔄 If HTTP/3 not working, deploy rpxy proxy
5. 🔄 Monitor performance metrics
6. 🔄 Update documentation with results

## References

- [Spring Boot 3.4 HTTP/3 Support](https://spring.io/blog/2024/11/26/http3-in-reactor-2024/)
- [Reactor Netty HTTP/3 Documentation](https://projectreactor.io/docs)
- [Railway rpxy Proxy Template](https://railway.com/deploy/rpxy-proxy)
- [Apple URLSession HTTP/3 Support](https://developer.apple.com/documentation/foundation/urlsession)
- [Netty HTTP/3 Codec](https://github.com/netty/netty-incubator-codec-http3)

## Questions?

Contact Railway support via:
- [Railway Discord](https://discord.gg/railway)
- [Railway Central Station](https://docs.railway.com/reference/support)

---

**Last Updated**: December 23, 2024
**Spring Boot Version**: 3.4.1
**Reactor BOM Version**: 2024.0.0
**Netty HTTP/3 Codec Version**: 0.0.28.Final

