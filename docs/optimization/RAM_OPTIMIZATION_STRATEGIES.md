# RAM Optimization Strategies for Spawn App Back-End

**Generated:** October 31, 2025  
**Spring Boot Version:** 3.3.5  
**Java Version:** 17

## Executive Summary

Based on analysis of your Spring Boot application, this document outlines specific strategies to reduce RAM usage across JVM configuration, database connection pooling, Redis caching, external libraries, and application code patterns.

---

## Table of Contents
1. [JVM & Garbage Collection Tuning](#1-jvm--garbage-collection-tuning)
2. [Database Connection Pool Optimization](#2-database-connection-pool-optimization)
3. [Redis Configuration Optimization](#3-redis-configuration-optimization)
4. [Hibernate/JPA Optimization](#4-hibernatejpa-optimization)
5. [Spring Boot Configuration](#5-spring-boot-configuration)
6. [External Library Optimization](#6-external-library-optimization)
7. [Application Code Patterns](#7-application-code-patterns)
8. [Monitoring & Profiling](#8-monitoring--profiling)

---

## 1. JVM & Garbage Collection Tuning

### Current State
- No explicit JVM heap settings in production
- Test profile uses: `-Xmx1g -Xms512m`

### Recommendations

#### A. Set Explicit Heap Limits
Add to your production startup script or deployment configuration:

```bash
# For a container with 2GB RAM available
java -Xms512m \
     -Xmx1536m \
     -XX:MaxMetaspaceSize=256m \
     -XX:CompressedClassSpaceSize=64m \
     -jar spawn.jar
```

**Rationale:**
- **-Xms512m**: Start with 512MB to avoid early GC cycles
- **-Xmx1536m**: Cap at 1.5GB (leave headroom for native memory)
- **MaxMetaspaceSize=256m**: Limit class metadata (you have ~264 Java files)
- **CompressedClassSpaceSize=64m**: Reduce compressed class pointer space

#### B. Use G1GC Garbage Collector (Default in Java 17)
G1GC is already the default in Java 17, but you can tune it:

```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=4m \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -Xms512m -Xmx1536m \
     -jar spawn.jar
```

**Rationale:**
- G1GC is designed for low-pause-time applications
- `MaxGCPauseMillis=200`: Target 200ms GC pauses (adjust based on requirements)
- `InitiatingHeapOccupancyPercent=45`: Start concurrent GC earlier to prevent full GCs

#### C. Enable String Deduplication
```bash
java -XX:+UseStringDeduplication \
     -XX:StringDeduplicationAgeThreshold=3 \
     -jar spawn.jar
```

**Rationale:** Your app handles many user strings (usernames, names, emails, activity titles). String deduplication can save 5-15% heap memory.

#### D. For Container/Cloud Deployments
```bash
# Docker/Kubernetes
java -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -XX:InitialRAMPercentage=25.0 \
     -jar spawn.jar
```

**Rationale:** Let JVM automatically detect container memory limits and set heap accordingly.

---

## 2. Database Connection Pool Optimization

### Current State (HikariCP)
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000  # 5 minutes
spring.datasource.hikari.max-lifetime=1200000 # 20 minutes
```

### Issues
- **20 connections** is too high for most workloads
- Each connection ~1-2MB RAM
- **Potential savings: ~20-30MB**

### Recommended Configuration

```properties
# Optimized HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=600000    # 10 minutes
spring.datasource.hikari.max-lifetime=1800000   # 30 minutes
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.leak-detection-threshold=60000

# Additional optimizations
spring.datasource.hikari.register-mbeans=false  # Disable JMX unless needed
spring.datasource.hikari.auto-commit=true
```

**Calculation for pool size:**
```
pool_size = (core_count × 2) + effective_spindle_count
# For 2-core system: (2 × 2) + 1 = 5
# For 4-core system: (4 × 2) + 1 = 9
# Use 10 as a safe maximum
```

**Rationale:**
- Most web apps don't need 20 concurrent DB connections
- Monitor actual usage; you'll likely see <5 active at peak
- Reduces memory overhead from idle connections

---

## 3. Redis Configuration Optimization

### Current State
- Default Redis serialization (likely JDK serialization)
- Multiple cache configurations with varying TTLs
- 19+ cache regions defined

### Issues
- **JDK serialization is inefficient** (~2-3x larger than alternatives)
- Large number of cache regions increases memory overhead
- No max cache size limits

### Recommendations

#### A. Switch to Efficient Serialization

Update `RedisCacheConfig.java`:

```java
@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
@Profile("!test")
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Use GenericJackson2JsonRedisSerializer for efficiency
        GenericJackson2JsonRedisSerializer serializer = 
            new GenericJackson2JsonRedisSerializer();
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(100))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()  // Don't cache null values
                .computePrefixWith(cacheName -> "spawn:" + cacheName + ":");
        
        // ... rest of configuration
    }
}
```

**Add required imports:**
```java
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
```

**Benefits:**
- **30-50% reduction** in serialized object size
- Human-readable cached data (easier debugging)
- Better compatibility with different Java versions

#### B. Add Memory Limits to Redis

In your `application.properties`:

```properties
# Redis Connection with maxmemory
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.password=${REDIS_PASSWORD}

# If you control the Redis server, configure maxmemory
# Via Redis CLI or config file:
# maxmemory 256mb
# maxmemory-policy allkeys-lru
```

#### C. Consider Redis Connection Pooling

Add to `application.properties`:

```properties
# Lettuce connection pool (default Redis client in Spring Boot)
spring.data.redis.lettuce.pool.enabled=true
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=4
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.max-wait=2000ms
```

**Add dependency** (if not already present):
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

#### D. Consolidate Cache Regions

**Current:** 19+ cache regions  
**Recommendation:** Group similar caches

```java
// Instead of multiple user-related caches, use one with compound keys
.withCacheConfiguration("userData", userDataConfig)

// Cache key format: userData::userId::type (e.g., "userData::123::interests")
```

This reduces memory overhead from managing multiple cache managers.

---

## 4. Hibernate/JPA Optimization

### Current State
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.jdbc.batch_size=25
spring.jpa.open-in-view=false  # ✅ Good!
```

### Recommendations

#### A. Enable Second-Level Cache (Conditional)

Only enable if you have truly static data:

```properties
# Enable second-level cache for read-mostly entities
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
spring.jpa.properties.hibernate.cache.use_query_cache=false  # Query cache can increase memory

# Cache only specific entities
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class ActivityType { ... }  // ActivityType is read-mostly
```

**Warning:** Only cache entities that change infrequently. You're already using Redis for caching DTOs, which is often better.

#### B. Optimize Entity Fetch Strategies

Review your entities for N+1 query issues:

```java
// Current User entity - ✅ Good, minimal relationships
@Entity
public class User {
    // No eager collections - good!
}

// Current Activity entity - ✅ Good, uses FetchType.LAZY
@Entity
public class Activity {
    @ManyToOne(fetch = FetchType.LAZY)
    private ActivityType activityType;  // ✅ Correct
}
```

**Your entities look well-optimized already!** Keep using `FetchType.LAZY` for all relationships.

#### C. Use Hibernate Statistics to Find Issues

Add to `application.properties` (dev/staging only):

```properties
# Enable Hibernate statistics logging
spring.jpa.properties.hibernate.generate_statistics=true
logging.level.org.hibernate.stat=DEBUG
logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN
```

Monitor for:
- High entity load counts
- Collection fetch counts
- Session cache size

#### D. Configure Hibernate Batch Fetching

```properties
# Already have batch_size=25 ✅
# Add these for collection optimization:
spring.jpa.properties.hibernate.default_batch_fetch_size=16
spring.jpa.properties.hibernate.jdbc.fetch_size=50
```

#### E. Statement Cache Configuration

```properties
# Cache prepared statements (MySQL)
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048
spring.datasource.hikari.data-source-properties.useServerPrepStmts=true
```

**Benefit:** Reduces memory and CPU for statement parsing.

---

## 5. Spring Boot Configuration

### Recommendations

#### A. Disable Unused Auto-Configurations

In `SpawnApplication.java`:

```java
@SpringBootApplication(exclude = {
    RedisRepositoriesAutoConfiguration.class,  // ✅ Already excluded
    // Add these if not used:
    JmxAutoConfiguration.class,                // Disable JMX if not monitoring via JMX
    WebSocketAutoConfiguration.class,          // If not using WebSockets
    DevToolsDataSourceAutoConfiguration.class  // Remove devtools in production
})
```

#### B. Optimize Jackson ObjectMapper

Your `JacksonConfig.java` is already well-configured! Consider adding:

```java
@Bean
@Primary
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    
    // Existing configuration...
    mapper.registerModule(hibernateModule);
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    
    // Add these for memory efficiency:
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    
    // Reduce JSON parsing memory overhead
    mapper.configure(JsonParser.Feature.CANONICALIZE_FIELD_NAMES, false);
    
    return mapper;
}
```

#### C. Optimize Thread Pools

Add to `application.properties`:

```properties
# Tomcat thread pool optimization
server.tomcat.threads.max=50          # Down from default 200
server.tomcat.threads.min-spare=5     # Down from default 10
server.tomcat.max-connections=2000    # Default is 8192 (too high)
server.tomcat.accept-count=100

# Connection timeout
server.tomcat.connection-timeout=20000

# Reduce keep-alive
server.tomcat.keep-alive-timeout=60000
server.tomcat.max-keep-alive-requests=100
```

**Each thread:** ~1-2MB stack + overhead  
**Potential savings:** 150 threads × 1.5MB = ~225MB

#### D. Async Configuration (If Using @Async)

```properties
# If using async processing
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
```

#### E. Logging Configuration

Your `logback-spring.xml` is minimal. Consider adding:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %highlight(%-5level) [%logger{36}:%line] %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Reduce logging overhead -->
    <turboFilter class="ch.qos.logback.classic.turbo.DuplicateMessageFilter"/>
    
    <root level="info">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <!-- Reduce verbose library logging -->
    <logger name="org.springframework.web" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="com.zaxxer.hikari" level="WARN"/>
    <logger name="io.lettuce.core" level="WARN"/>
</configuration>
```

---

## 6. External Library Optimization

### Current Dependencies Analysis

#### A. AWS SDK v2 Optimization

**Current:** Using full AWS SDK S3
**Optimization:** SDK v2 is already optimized, but ensure you're using the latest version

Update `pom.xml`:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.28.0</version>  <!-- Update from 2.25.27 -->
</dependency>
```

**Consider:** If S3 is your only AWS service, the SDK is already minimal.

**Configuration:** Add to `S3Config.java`:

```java
@Bean
public S3Client s3Client() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
    
    return S3Client.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            // Add these for memory efficiency:
            .overrideConfiguration(b -> b
                .apiCallTimeout(Duration.ofSeconds(30))
                .apiCallAttemptTimeout(Duration.ofSeconds(10)))
            .build();
}
```

#### B. Firebase Admin SDK Optimization

**Issue:** Firebase SDK can be memory-heavy (~50-100MB)

**Option 1:** Use HTTP Client instead (if only using FCM):

```xml
<!-- Remove -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.4.1</version>
</dependency>

<!-- Replace with lightweight HTTP client for FCM -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

**Option 2:** If you need Firebase SDK, initialize lazily:

```java
@Service
@Profile("!test")
public class FCMService {
    
    private volatile FirebaseApp firebaseApp;
    
    private FirebaseApp getFirebaseApp() {
        if (firebaseApp == null) {
            synchronized (this) {
                if (firebaseApp == null) {
                    // Initialize only when first needed
                    firebaseApp = initializeFirebase();
                }
            }
        }
        return firebaseApp;
    }
}
```

#### C. Review Google API Client

**Current:**
```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.4.0</version>
</dependency>
```

**Issue:** Pulls in many transitive dependencies

**Check:** Do you only need token verification? Consider using:

```xml
<!-- Lighter alternative for Google Sign-In verification -->
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.23.0</version>
</dependency>
```

#### D. Twilio SDK

**Current:**
```xml
<dependency>
    <groupId>com.twilio.sdk</groupId>
    <artifactId>twilio</artifactId>
    <version>10.9.1</version>
</dependency>
```

**Optimization:** Twilio SDK is reasonably lightweight. No action needed unless you can use their REST API directly with a lightweight HTTP client.

---

## 7. Application Code Patterns

### A. DTO Serialization Best Practices

**Current State:** DTOs implement `Serializable` for Redis ✅

**Optimization:** Review DTO size. Avoid caching large objects:

```java
// Good: Small, focused DTO
public class UserSummaryDTO implements Serializable {
    private UUID id;
    private String username;
    private String name;
    // Only essential fields
}

// Bad: Don't cache full user graphs
public class FullUserWithEverythingDTO implements Serializable {
    private UUID id;
    private String username;
    private List<ActivityDTO> allActivities;     // ❌ Can be huge!
    private List<FriendDTO> allFriends;          // ❌ Can be huge!
    private List<ChatMessageDTO> allMessages;    // ❌ Can be huge!
}
```

**Action:** Review cached DTOs in `RedisCacheConfig` and ensure they're lightweight.

### B. Pagination for Large Collections

Ensure all list endpoints use pagination:

```java
@GetMapping("/activities")
public ResponseEntity<Page<ActivityDTO>> getActivities(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "50") int maxSize) {
    
    // Limit max page size to prevent memory issues
    size = Math.min(size, maxSize);
    
    Pageable pageable = PageRequest.of(page, size);
    return ResponseEntity.ok(activityService.getActivities(pageable));
}
```

### C. Stream Processing for Large Datasets

Use Java Streams with care:

```java
// Bad: Loads all users into memory
List<User> allUsers = userRepository.findAll();
List<UserDTO> dtos = allUsers.stream()
    .map(userMapper::toDTO)
    .collect(Collectors.toList());

// Good: Process in batches or use pagination
Page<User> userPage = userRepository.findAll(pageable);
List<UserDTO> dtos = userPage.stream()
    .map(userMapper::toDTO)
    .collect(Collectors.toList());
```

### D. Implement Cache Eviction Strategy

Review `CacheService.java` to ensure proper eviction:

```java
@Service
public class CacheService {
    
    @Autowired
    private CacheManager cacheManager;
    
    // Clear specific user caches when user data changes
    public void evictUserCaches(UUID userId) {
        evictCache("userData", userId.toString());
        evictCache("userStats", userId.toString());
        // Only evict what's necessary, not all caches
    }
    
    // Schedule periodic cache cleanup
    @Scheduled(cron = "0 0 3 * * ?") // 3 AM daily
    public void cleanupStaleCaches() {
        // Clear all caches at low-traffic time
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}
```

### E. Request Scoped Beans (Use Sparingly)

Avoid creating request-scoped beans unnecessarily:

```java
// Bad: Creates new instance per request
@Bean
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public SomeService someService() { ... }

// Good: Singleton for stateless services
@Service
public class SomeService { ... }
```

---

## 8. Monitoring & Profiling

### A. Add Actuator Metrics (Production-Safe)

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Configure in `application.properties`:

```properties
# Actuator configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=when-authorized
management.metrics.enable.jvm=true
management.metrics.enable.tomcat=true
management.metrics.enable.hikaricp=true

# Secure actuator endpoints
management.endpoints.web.base-path=/actuator
management.server.port=8081  # Different port for security
```

### B. Memory Monitoring Endpoints

Access these to monitor memory:

- `GET /actuator/metrics/jvm.memory.used`
- `GET /actuator/metrics/jvm.memory.max`
- `GET /actuator/metrics/hikaricp.connections.active`
- `GET /actuator/metrics/cache.size`

### C. Enable Heap Dumps (Staging/Dev Only)

```bash
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/spawn/heapdump.hprof \
     -Xms512m -Xmx1536m \
     -jar spawn.jar
```

### D. Use Java Flight Recorder (JFR)

```bash
# Enable JFR recording
java -XX:StartFlightRecording=duration=60s,filename=/tmp/spawn-recording.jfr \
     -jar spawn.jar

# Analyze with Java Mission Control or jfr CLI
jfr summary /tmp/spawn-recording.jfr
```

### E. Recommended Monitoring Tools

1. **VisualVM** - Free, good for local profiling
2. **JProfiler** - Commercial, excellent for memory leak detection
3. **Prometheus + Grafana** - Metrics collection and visualization
4. **New Relic / DataDog APM** - Commercial, comprehensive monitoring

---

## Implementation Priority

### Phase 1: Quick Wins (1-2 days)
✅ **High Impact, Low Effort**

1. ✅ Reduce HikariCP pool size from 20 → 10
2. ✅ Reduce Tomcat threads from 200 → 50
3. ✅ Set explicit JVM heap limits (-Xmx1536m)
4. ✅ Enable String Deduplication
5. ✅ Add Redis connection pooling

**Expected Savings:** 150-250MB RAM

---

### Phase 2: Medium Term (1 week)
⚠️ **Medium Impact, Moderate Effort**

6. ⚠️ Switch Redis serialization to Jackson (from JDK)
7. ⚠️ Add Actuator for monitoring
8. ⚠️ Review and consolidate cache regions
9. ⚠️ Optimize logging configuration
10. ⚠️ Add maxmemory limits to Redis

**Expected Savings:** 50-100MB RAM + improved efficiency

---

### Phase 3: Long Term (2-4 weeks)
🔧 **Variable Impact, High Effort**

11. 🔧 Profile with JFR to identify memory hotspots
12. 🔧 Review all DTOs for size optimization
13. 🔧 Consider replacing Firebase SDK with HTTP client
14. 🔧 Review Google API client usage (use lighter alternatives)
15. 🔧 Implement cache eviction strategies
16. 🔧 Add pagination to all list endpoints (if not present)

**Expected Savings:** 100-200MB RAM + improved scalability

---

## Configuration File Changes Summary

### application.properties
```properties
# JPA/Hibernate - Add these
spring.jpa.properties.hibernate.default_batch_fetch_size=16
spring.jpa.properties.hibernate.jdbc.fetch_size=50

# HikariCP - Update these
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.register-mbeans=false

# Tomcat - Add these
server.tomcat.threads.max=50
server.tomcat.threads.min-spare=5
server.tomcat.max-connections=2000
server.tomcat.accept-count=100

# Redis - Add these
spring.data.redis.lettuce.pool.enabled=true
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=4
spring.data.redis.lettuce.pool.min-idle=2

# Actuator - Add these
management.endpoints.web.exposure.include=health,metrics
management.metrics.enable.jvm=true
```

### Startup Script / Dockerfile
```bash
#!/bin/bash
java -Xms512m \
     -Xmx1536m \
     -XX:MaxMetaspaceSize=256m \
     -XX:+UseStringDeduplication \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -jar spawn.jar
```

---

## Measuring Success

### Baseline Metrics (Measure Before Changes)
1. Heap memory used at startup
2. Heap memory after 1 hour of normal traffic
3. Max heap memory during peak traffic
4. Number of active database connections
5. Cache hit rate
6. Average response time

### Target Metrics (After Optimization)
- **Heap Memory:** Reduce by 30-40% (300-400MB savings)
- **Database Connections:** <10 active at peak
- **GC Pause Time:** <200ms
- **Cache Hit Rate:** >80%
- **Response Time:** Maintain or improve

### Monitoring Commands

```bash
# Check current heap usage
jmap -heap <PID>

# Monitor GC activity
jstat -gcutil <PID> 1000

# Check thread count
jstack <PID> | grep "^\"" | wc -l

# Monitor active DB connections (via actuator)
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

---

## Additional Resources

- [HikariCP Configuration Guide](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Spring Boot Memory Tuning](https://spring.io/blog/2015/12/10/spring-boot-memory-performance)
- [G1GC Tuning Guide](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [Redis Memory Optimization](https://redis.io/docs/management/optimization/memory-optimization/)

---

## Questions or Concerns?

If implementing any of these strategies causes issues:
1. Roll back the change immediately
2. Check application logs for errors
3. Review actuator metrics for anomalies
4. Test under load in a staging environment first

**Remember:** Make one change at a time and measure the impact before proceeding to the next optimization.

