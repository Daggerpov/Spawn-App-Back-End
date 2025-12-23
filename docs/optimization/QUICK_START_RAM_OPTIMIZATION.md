# Quick Start: RAM Optimization Implementation Guide

**🎯 Priority: Implement these changes first for immediate RAM savings of 150-250MB**

---

## 1. Update application.properties

Add these lines to your `src/main/resources/application.properties`:

```properties
# ============================================================================
# RAM OPTIMIZATION CHANGES - Added [DATE]
# ============================================================================

# 1. REDUCE HIKARI CONNECTION POOL (Saves ~20-30MB)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.register-mbeans=false

# 2. OPTIMIZE TOMCAT THREADS (Saves ~150-200MB)
server.tomcat.threads.max=50
server.tomcat.threads.min-spare=5
server.tomcat.max-connections=2000
server.tomcat.accept-count=100
server.tomcat.connection-timeout=20000

# 3. REDIS CONNECTION POOLING
spring.data.redis.lettuce.pool.enabled=true
spring.data.redis.lettuce.pool.max-active=8
spring.data.redis.lettuce.pool.max-idle=4
spring.data.redis.lettuce.pool.min-idle=2
spring.data.redis.lettuce.pool.max-wait=2000ms

# 4. HIBERNATE OPTIMIZATIONS
spring.jpa.properties.hibernate.default_batch_fetch_size=16
spring.jpa.properties.hibernate.jdbc.fetch_size=50

# 5. STATEMENT CACHE (MySQL)
spring.datasource.hikari.data-source-properties.cachePrepStmts=true
spring.datasource.hikari.data-source-properties.prepStmtCacheSize=250
spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit=2048
spring.datasource.hikari.data-source-properties.useServerPrepStmts=true
```

---

## 2. Update pom.xml

Add Apache Commons Pool2 for Redis connection pooling:

```xml
<!-- Add this dependency after the Redis starter -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

---

## 3. Update RedisCacheConfig.java

Replace the entire `cacheManager()` method in `src/main/java/com/danielagapov/spawn/Config/RedisCacheConfig.java`:

```java
package com.danielagapov.spawn.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
@Profile("!test")
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Use JSON serialization instead of JDK serialization (30-50% size reduction)
        GenericJackson2JsonRedisSerializer serializer = 
            new GenericJackson2JsonRedisSerializer();
        
        // Set default TTL of 100 minutes
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(100))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()  // Don't cache null values
                .computePrefixWith(cacheName -> "spawn:" + cacheName + ":");

        // Configure different TTL values for different cache types
        RedisCacheConfiguration userDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        
        RedisCacheConfiguration staticDataConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(4))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        
        RedisCacheConfiguration statsConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
        
        RedisCacheConfiguration activityConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                // User-related caches
                .withCacheConfiguration("friendsByUserId", userDataConfig)
                .withCacheConfiguration("recommendedFriends", userDataConfig)
                .withCacheConfiguration("userInterests", userDataConfig)
                .withCacheConfiguration("userSocialMedia", userDataConfig)
                .withCacheConfiguration("userSocialMediaByUserId", userDataConfig)
                
                // Friend request caches
                .withCacheConfiguration("incomingFetchFriendRequests", userDataConfig)
                .withCacheConfiguration("sentFetchFriendRequests", userDataConfig)
                .withCacheConfiguration("friendRequests", userDataConfig)
                .withCacheConfiguration("friendRequestsByUserId", userDataConfig)
                
                // Activity type caches
                .withCacheConfiguration("activityTypes", staticDataConfig)
                .withCacheConfiguration("activityTypesByUserId", staticDataConfig)
                
                // Location caches
                .withCacheConfiguration("locations", staticDataConfig)
                .withCacheConfiguration("locationById", staticDataConfig)
                
                // Stats caches
                .withCacheConfiguration("userStats", statsConfig)
                .withCacheConfiguration("userStatsById", statsConfig)
                
                // Activity caches
                .withCacheConfiguration("ActivityById", activityConfig)
                .withCacheConfiguration("fullActivityById", activityConfig)
                .withCacheConfiguration("ActivityInviteById", activityConfig)
                .withCacheConfiguration("ActivitiesByOwnerId", activityConfig)
                .withCacheConfiguration("feedActivities", activityConfig)
                .withCacheConfiguration("ActivitiesInvitedTo", activityConfig)
                .withCacheConfiguration("fullActivitiesInvitedTo", activityConfig)
                .withCacheConfiguration("fullActivitiesParticipatingIn", activityConfig)
                .withCacheConfiguration("calendarActivities", activityConfig)
                .withCacheConfiguration("allCalendarActivities", activityConfig)
                .withCacheConfiguration("filteredCalendarActivities", activityConfig)
                
                // Blocked user caches
                .withCacheConfiguration("blockedUsers", userDataConfig)
                .withCacheConfiguration("blockedUserIds", userDataConfig)
                .withCacheConfiguration("isBlocked", userDataConfig)
                .build();
    }
}
```

---

## 4. Update logback-spring.xml

Replace `src/main/resources/logback-spring.xml` with:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} %highlight(%-5level) [%logger{36}:%line] %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Reduce duplicate log messages -->
    <turboFilter class="ch.qos.logback.classic.turbo.DuplicateMessageFilter"/>
    
    <root level="info">
        <appender-ref ref="CONSOLE"/>
    </root>
    
    <!-- Reduce verbose library logging to save memory -->
    <logger name="org.springframework.web" level="WARN"/>
    <logger name="org.hibernate" level="WARN"/>
    <logger name="com.zaxxer.hikari" level="INFO"/>  <!-- Keep INFO for connection pool monitoring -->
    <logger name="io.lettuce.core" level="WARN"/>
    <logger name="software.amazon.awssdk" level="WARN"/>
</configuration>
```

---

## 5. Create/Update Startup Script

Create `scripts/start-production.sh`:

```bash
#!/bin/bash

# Spawn App - Production Startup Script with RAM Optimizations
# Usage: ./start-production.sh

# Set JVM memory limits and optimizations
export JAVA_OPTS="-Xms512m \
                  -Xmx1536m \
                  -XX:MaxMetaspaceSize=256m \
                  -XX:CompressedClassSpaceSize=64m \
                  -XX:+UseStringDeduplication \
                  -XX:StringDeduplicationAgeThreshold=3 \
                  -XX:+UseG1GC \
                  -XX:MaxGCPauseMillis=200 \
                  -XX:InitiatingHeapOccupancyPercent=45 \
                  -XX:+UseContainerSupport \
                  -XX:MaxRAMPercentage=75.0 \
                  -XX:+HeapDumpOnOutOfMemoryError \
                  -XX:HeapDumpPath=/var/log/spawn/heapdump.hprof"

# Start the application
java $JAVA_OPTS -jar target/spawn-0.0.1-SNAPSHOT.jar
```

Make it executable:
```bash
chmod +x scripts/start-production.sh
```

---

## 6. For Docker Deployments

Update your `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/spawn-0.0.1-SNAPSHOT.jar app.jar

# Set JVM options for container
ENV JAVA_OPTS="-Xms512m \
               -Xmx1536m \
               -XX:MaxMetaspaceSize=256m \
               -XX:+UseStringDeduplication \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Important:** If using Kubernetes or Docker Compose, set container memory limit to **2GB** to allow headroom:

```yaml
# docker-compose.yml
services:
  spawn-app:
    image: spawn-app:latest
    mem_limit: 2g
    mem_reservation: 1g
```

---

## 7. Testing the Changes

### Step 1: Build the application
```bash
mvn clean package -DskipTests
```

### Step 2: Start with monitoring
```bash
# Terminal 1: Start the application
./scripts/start-production.sh

# Terminal 2: Monitor memory usage
watch -n 5 'jps | grep spawn | awk "{print \$1}" | xargs jstat -gcutil'
```

### Step 3: Run a load test (optional)
```bash
# Use Apache Bench or similar tool
ab -n 1000 -c 10 http://localhost:8080/api/health
```

### Step 4: Check metrics
```bash
# View heap usage
jps | grep spawn | awk '{print $1}' | xargs jmap -heap

# View thread count
jps | grep spawn | awk '{print $1}' | xargs jstack | grep "^\"" | wc -l

# View GC statistics
jps | grep spawn | awk '{print $1}' | xargs jstat -gc
```

---

## 8. Rollback Plan

If you encounter issues, revert these changes:

### Revert application.properties
```bash
git checkout src/main/resources/application.properties
```

### Revert RedisCacheConfig.java
```bash
git checkout src/main/java/com/danielagapov/spawn/Config/RedisCacheConfig.java
```

### Restart with default settings
```bash
java -jar target/spawn-0.0.1-SNAPSHOT.jar
```

---

## Expected Results

### Before Optimization
- **Heap Memory:** 800-1200MB
- **Active Threads:** 200+
- **DB Connections:** 15-20 active
- **GC Frequency:** Every 30-60 seconds

### After Optimization
- **Heap Memory:** 500-800MB (25-40% reduction)
- **Active Threads:** 50-70
- **DB Connections:** 5-10 active
- **GC Frequency:** Every 60-120 seconds

---

## Monitoring Commands

```bash
# Memory usage
jps | grep spawn | awk '{print $1}' | xargs jmap -heap

# Thread count
jps | grep spawn | awk '{print $1}' | xargs jstack | grep "^\"" | wc -l

# GC statistics (live monitoring every 1 second)
jps | grep spawn | awk '{print $1}' | xargs jstat -gcutil 1000

# HikariCP connection pool stats (via logs)
tail -f logs/application.log | grep HikariCP

# Top memory-consuming classes
jps | grep spawn | awk '{print $1}' | xargs jmap -histo | head -n 30
```

---

## Next Steps

After implementing these changes and verifying stability:

1. ✅ Monitor for 24-48 hours in production
2. ✅ Review logs for any connection pool issues
3. ✅ Check cache hit rates (should remain high)
4. ✅ Proceed to Phase 2 optimizations (see RAM_OPTIMIZATION_STRATEGIES.md)

---

## Common Issues & Solutions

### Issue 1: "Cannot get connection from pool"
**Solution:** Increase max-pool-size from 10 to 15
```properties
spring.datasource.hikari.maximum-pool-size=15
```

### Issue 2: "Redis connection timeout"
**Solution:** Increase Redis pool max-active
```properties
spring.data.redis.lettuce.pool.max-active=12
```

### Issue 3: "OutOfMemoryError: Metaspace"
**Solution:** Increase MaxMetaspaceSize
```bash
-XX:MaxMetaspaceSize=384m
```

### Issue 4: Response times increased
**Solution:** Check if you need more threads
```properties
server.tomcat.threads.max=75
```

---

## Support

If you encounter any issues:
1. Check application logs: `tail -f logs/application.log`
2. Review the full RAM_OPTIMIZATION_STRATEGIES.md document
3. Revert changes and gradually re-apply one at a time
4. Use `jstack` and `jmap` to diagnose memory issues

**Remember:** Always test in a staging environment before deploying to production!

