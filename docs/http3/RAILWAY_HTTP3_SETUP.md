# Railway HTTP/3 Quick Setup Guide

## TL;DR

Railway **may not support HTTP/3 natively**. Two options:

### Option 1: Standard Deploy (Test First) ⚡

```bash
git push  # Deploy and test if Railway supports HTTP/3
```

Test with:
```bash
curl --http3 -I https://your-app.up.railway.app/api/v1/health
```

✅ If you see "HTTP/3 200" → You're done!  
❌ If you see "HTTP/2 200" or "HTTP/1.1 200" → Use Option 2

---

### Option 2: Deploy with rpxy Proxy (Guaranteed HTTP/3) 🚀

#### Step 1: Deploy rpxy Proxy

1. Go to: https://railway.com/deploy/rpxy-proxy
2. Click "Deploy Now"
3. Select your Railway project
4. Deploy

#### Step 2: Configure rpxy

Create `rpxy.toml` in your rpxy service:

```toml
[app]
server_name = "spawn-app-back-end-production.up.railway.app"

[[app.reverse_proxy]]
upstream = "your-spring-boot-service.railway.internal:8080"

[app.tls]
https_redirection = true
tls_versions = ["1.3"]

[app.http3]
enabled = true
```

#### Step 3: Update DNS

Point your domain to the rpxy proxy URL instead of directly to Spring Boot.

#### Step 4: Update iOS App

In `ServiceConstants.swift`:

```swift
static let apiBase = "https://your-rpxy-proxy.up.railway.app/api/v1/"
```

#### Step 5: Test

```bash
curl --http3 -I https://your-rpxy-proxy.up.railway.app/api/v1/health
# Should return: HTTP/3 200
```

---

## Railway Environment Variables

Ensure these are set in Railway:

```bash
# Required for production
MYSQL_URL=your-mysql-url
MYSQL_USER=your-user
MYSQL_PASSWORD=your-password
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# SSL/TLS (Railway usually handles this automatically)
# No manual configuration needed for Railway-provided domains
```

---

## Verification Commands

### Test HTTP/3 Support
```bash
curl --http3 -I https://your-app.up.railway.app/api/v1/health
```

### Test HTTP/2 Fallback
```bash
curl --http2 -I https://your-app.up.railway.app/api/v1/health
```

### Test HTTP/1.1 Fallback
```bash
curl --http1.1 -I https://your-app.up.railway.app/api/v1/health
```

### Check Protocol in Chrome
1. Open DevTools → Network tab
2. Right-click columns → Add "Protocol"
3. Make request
4. Look for "h3" (HTTP/3), "h2" (HTTP/2), or "http/1.1"

---

## Contact Railway Support

If you need to confirm HTTP/3 support:

- **Discord**: https://discord.gg/railway
- **Central Station**: https://docs.railway.com/reference/support
- **Question**: "Does Railway support HTTP/3 (QUIC) passthrough for Spring Boot applications?"

---

## Expected Performance

With HTTP/3 enabled:

| Scenario | Improvement |
|----------|-------------|
| Initial connection | 50-100ms faster |
| Poor network (3G/4G) | 10-30% better throughput |
| WiFi → Cellular transition | Seamless (no reconnection) |
| Parallel API calls | Reduced latency |

---

## Troubleshooting

### "curl: option --http3: is unknown"

Install curl with HTTP/3 support:

**macOS**:
```bash
brew install curl-openssl
/opt/homebrew/opt/curl/bin/curl --http3 -I https://your-app.up.railway.app
```

**Linux**:
```bash
# Use Docker
docker run --rm curlimages/curl:latest --http3 -I https://your-app.up.railway.app
```

### "HTTP/2 200" instead of "HTTP/3 200"

Railway may not support HTTP/3 passthrough. Use **Option 2: rpxy proxy**.

### Build fails on Railway

Check Railway build logs:
```bash
railway logs
```

Common issues:
- Java version mismatch (needs Java 17+)
- Maven dependency resolution issues
- Memory limits (increase in Railway settings)

---

## Quick Links

- **Full Documentation**: `docs/HTTP3_IMPLEMENTATION.md`
- **Upgrade Summary**: `HTTP3_UPGRADE_SUMMARY.md`
- **rpxy Template**: https://railway.com/deploy/rpxy-proxy
- **Railway Docs**: https://docs.railway.com

---

**Last Updated**: December 23, 2024

