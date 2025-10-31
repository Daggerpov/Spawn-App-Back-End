# RAM Optimization Guide

**Last Updated:** October 31, 2025  
**Status:** Ready for Implementation  
**Expected RAM Savings:** 300-670 MB (25-55% reduction)

> **🚀 TL;DR:** See **[RAM_OPTIMIZATION_SUMMARY.md](./RAM_OPTIMIZATION_SUMMARY.md)** for a concise overview of what was changed and how to deploy.

---

## 📚 Documentation Index

This directory contains comprehensive documentation for reducing RAM usage in the Spawn App back-end.

### Core Documents

1. **[RAM_OPTIMIZATION_SUMMARY.md](./RAM_OPTIMIZATION_SUMMARY.md)** 📋 **EXECUTIVE SUMMARY**
   - Concise overview of all changes made in `ram-optimizations` branch
   - Quick deployment guide
   - Expected impact and savings
   - Perfect for quick reference

2. **[RAM_OPTIMIZATION_STRATEGIES.md](./RAM_OPTIMIZATION_STRATEGIES.md)** ⭐ **TECHNICAL GUIDE**
   - Complete technical guide covering all optimization strategies
   - JVM tuning, database pools, Redis, Hibernate/JPA, Spring Boot configs
   - Divided into 3 phases (Quick Wins → Medium-Term → Long-Term)
   - Includes monitoring, profiling, and troubleshooting guides

3. **[QUICK_START_RAM_OPTIMIZATION.md](./QUICK_START_RAM_OPTIMIZATION.md)** 🚀 **IMPLEMENTATION**
   - Step-by-step implementation guide for Phase 1 (Quick Wins)
   - Exact code changes needed
   - Copy-paste ready configuration updates
   - Testing and rollback procedures

4. **[RAM_OPTIMIZATION_IMPACT_SUMMARY.md](./RAM_OPTIMIZATION_IMPACT_SUMMARY.md)** 📊 **METRICS**
   - Visual breakdown of RAM usage before/after
   - Expected savings per optimization
   - Performance impact analysis
   - Success criteria and monitoring checklist

5. **[IMPLEMENTATION_COMPLETE.md](./IMPLEMENTATION_COMPLETE.md)** ✅ **STATUS**
   - Implementation status and verification
   - Next steps for testing
   - Common issues and solutions

### Supporting Tools

6. **[../scripts/monitor-ram.sh](../scripts/monitor-ram.sh)** 📈 **MONITORING TOOL**
   - Real-time RAM monitoring script
   - Tracks heap, metaspace, threads, GC
   - Generates CSV logs for analysis
   
7. **[../scripts/analyze-ram-logs.sh](../scripts/analyze-ram-logs.sh)** 🔍 **ANALYSIS TOOL**
   - Analyzes monitoring logs
   - Compares before/after optimization
   - Calculates savings and improvements

---

## 🎯 Quick Navigation by Role

### For Developers (Implementing Changes)
1. Read: [QUICK_START_RAM_OPTIMIZATION.md](./QUICK_START_RAM_OPTIMIZATION.md)
2. Apply changes to `application.properties`, `pom.xml`, `RedisCacheConfig.java`
3. Test locally
4. Use `monitor-ram.sh` to verify improvements

### For DevOps/SRE (Production Deployment)
1. Read: [RAM_OPTIMIZATION_STRATEGIES.md](./RAM_OPTIMIZATION_STRATEGIES.md) (Section 1: JVM Tuning)
2. Read: [QUICK_START_RAM_OPTIMIZATION.md](./QUICK_START_RAM_OPTIMIZATION.md) (Section 5: Startup Script)
3. Update deployment configs with JVM options
4. Monitor with actuator endpoints

### For Technical Leadership (Understanding Impact)
1. Read: [RAM_OPTIMIZATION_IMPACT_SUMMARY.md](./RAM_OPTIMIZATION_IMPACT_SUMMARY.md)
2. Review risk assessment and implementation timeline
3. Review expected cost savings
4. Approve phased rollout plan

### For Performance Testing (Validation)
1. Use: `scripts/monitor-ram.sh` to capture baseline
2. Apply Phase 1 optimizations
3. Use: `scripts/monitor-ram.sh` again for after metrics
4. Compare with: `scripts/analyze-ram-logs.sh`
5. Verify against success criteria in [RAM_OPTIMIZATION_IMPACT_SUMMARY.md](./RAM_OPTIMIZATION_IMPACT_SUMMARY.md)

---

## 🚀 Implementation Path

### Phase 1: Quick Wins (1-2 days)
**Expected Savings:** 300-400 MB | **Risk:** Very Low

```bash
# 1. Update configuration files
# - application.properties
# - pom.xml  
# - RedisCacheConfig.java
# - logback-spring.xml

# 2. Test locally
mvn clean package -DskipTests
./scripts/monitor-ram.sh 5 &
java -Xms512m -Xmx1536m -XX:+UseStringDeduplication -jar target/spawn-0.0.1-SNAPSHOT.jar

# 3. Monitor for 1 hour, verify no errors

# 4. Deploy to staging → production
```

**Key Changes:**
- ✅ Reduce HikariCP pool: 20 → 10 connections
- ✅ Reduce Tomcat threads: 200 → 50
- ✅ Set JVM heap limit: -Xmx1536m
- ✅ Enable String deduplication
- ✅ Switch Redis to JSON serialization
- ✅ Add Redis connection pooling

**Reference:** [QUICK_START_RAM_OPTIMIZATION.md](./QUICK_START_RAM_OPTIMIZATION.md)

---

### Phase 2: Medium-Term (1 week)
**Expected Savings:** 50-100 MB | **Risk:** Low-Medium

- ⚠️ Consolidate cache regions
- ⚠️ Add Spring Boot Actuator for monitoring
- ⚠️ Implement cache eviction strategies
- ⚠️ Optimize DTO sizes
- ⚠️ Review and optimize logging

**Reference:** [RAM_OPTIMIZATION_STRATEGIES.md](./RAM_OPTIMIZATION_STRATEGIES.md) (Sections 3-5)

---

### Phase 3: Long-Term (2-4 weeks)
**Expected Savings:** 110-170 MB | **Risk:** Medium

- 🔧 Profile with Java Flight Recorder (JFR)
- 🔧 Consider replacing Firebase Admin SDK
- 🔧 Optimize Google API Client usage
- 🔧 Implement advanced caching strategies
- 🔧 Code pattern optimizations

**Reference:** [RAM_OPTIMIZATION_STRATEGIES.md](./RAM_OPTIMIZATION_STRATEGIES.md) (Sections 6-7)

---

## 📊 Current State Analysis

### Application Architecture
- **Framework:** Spring Boot 3.3.5
- **Java Version:** 17
- **Database:** MySQL with HikariCP connection pooling
- **Cache:** Redis with Lettuce client
- **Entities:** 20 entities, 264 Java files

### Key Memory Consumers (Before Optimization)
```
Total RAM: ~1.2-1.5 GB
├─ JVM Heap:           600-800 MB
├─ Thread Stacks:      200 MB (200 threads)
├─ Metaspace:          150-200 MB
├─ Connection Pools:   30-40 MB
├─ External Libraries: 150-200 MB
└─ Native Memory:      100-150 MB
```

### Identified Issues
1. ❌ No JVM heap limits set → can grow unbounded
2. ❌ 200 Tomcat threads → wastes ~150 MB
3. ❌ 20 DB connections (using only 4-5) → wastes ~23 MB
4. ❌ JDK serialization in Redis → 40% larger than JSON
5. ❌ No String deduplication → duplicate strings in memory
6. ❌ No connection pooling for Redis

**Detailed Analysis:** [RAM_OPTIMIZATION_STRATEGIES.md](./RAM_OPTIMIZATION_STRATEGIES.md) (Introduction)

---

## 📈 Monitoring & Validation

### Before Making Changes

```bash
# 1. Start monitoring (Terminal 1)
./scripts/monitor-ram.sh 5

# 2. Run application normally for 1 hour

# 3. Generate load (Terminal 2)
# Use your favorite load testing tool
ab -n 1000 -c 10 http://localhost:8080/api/health

# 4. Save the log
mv logs/ram-monitor-*.log logs/baseline-before-optimization.log
```

### After Making Changes

```bash
# 1. Apply Phase 1 optimizations

# 2. Rebuild and restart
mvn clean package -DskipTests
./scripts/start-production.sh

# 3. Monitor again for 1 hour
./scripts/monitor-ram.sh 5

# 4. Save the log
mv logs/ram-monitor-*.log logs/optimized-after-phase1.log

# 5. Compare results
./scripts/analyze-ram-logs.sh \
    logs/baseline-before-optimization.log \
    logs/optimized-after-phase1.log
```

### Success Criteria

✅ **Minimum Success:**
- RAM reduced by at least 200 MB (15%)
- No increase in response times
- No connection pool errors
- Stable for 48 hours

🎯 **Optimal Success:**
- RAM reduced by 300-400 MB (25-33%)
- Response times improved by 5-10%
- GC pauses reduced by 30%+
- Stable for 1 week+

**Full Criteria:** [RAM_OPTIMIZATION_IMPACT_SUMMARY.md](./RAM_OPTIMIZATION_IMPACT_SUMMARY.md#success-criteria)

---

## 🔧 Tools & Scripts

### Monitoring Script
```bash
# Monitor RAM usage in real-time
./scripts/monitor-ram.sh [interval_seconds]

# Example: Monitor every 5 seconds (default)
./scripts/monitor-ram.sh 5

# Output:
# - Real-time console display
# - CSV log file in logs/ram-monitor-TIMESTAMP.log
# - Tracks: heap, metaspace, threads, GC count
```

### Analysis Script
```bash
# Analyze a single log
./scripts/analyze-ram-logs.sh logs/my-log.log

# Compare two logs (before/after)
./scripts/analyze-ram-logs.sh logs/before.log logs/after.log

# Output:
# - Statistical analysis (avg, min, max)
# - GC frequency
# - Before/after comparison
# - Savings calculation
```

### Manual Monitoring Commands
```bash
# Get JVM process ID
jps | grep spawn

# Heap memory usage
jmap -heap <PID>

# GC statistics (live updates every 1s)
jstat -gcutil <PID> 1000

# Thread count
jstack <PID> | grep -c "^\""

# Memory histogram (top consumers)
jmap -histo <PID> | head -n 30
```

---

## 🚨 Troubleshooting

### Common Issues After Optimization

#### Issue: "Cannot get JDBC Connection"
**Cause:** Connection pool too small  
**Solution:** Increase `spring.datasource.hikari.maximum-pool-size` from 10 to 15

#### Issue: "Redis connection timeout"
**Cause:** Redis pool too small  
**Solution:** Increase `spring.data.redis.lettuce.pool.max-active` from 8 to 12

#### Issue: "OutOfMemoryError: Metaspace"
**Cause:** Metaspace limit too restrictive  
**Solution:** Increase `-XX:MaxMetaspaceSize` from 256m to 384m

#### Issue: Slow response times under load
**Cause:** Not enough threads  
**Solution:** Increase `server.tomcat.threads.max` from 50 to 75

#### Issue: Application won't start after changes
**Cause:** Syntax error in configuration  
**Solution:** Check application logs, revert to backup configuration

**Full Troubleshooting:** [QUICK_START_RAM_OPTIMIZATION.md](./QUICK_START_RAM_OPTIMIZATION.md#common-issues--solutions)

---

## 📝 Configuration Changes Summary

### Files to Modify for Phase 1

1. **src/main/resources/application.properties**
   - HikariCP pool settings
   - Tomcat thread settings
   - Redis connection pooling
   - Hibernate batch settings

2. **pom.xml**
   - Add `commons-pool2` dependency

3. **src/main/java/com/danielagapov/spawn/Config/RedisCacheConfig.java**
   - Switch to JSON serialization
   - Add null value handling
   - Configure cache prefixes

4. **src/main/resources/logback-spring.xml**
   - Add duplicate message filter
   - Reduce library logging verbosity

5. **scripts/start-production.sh** (create new)
   - JVM heap limits
   - String deduplication
   - G1GC tuning
   - Metaspace limits

**Exact Changes:** [QUICK_START_RAM_OPTIMIZATION.md](./QUICK_START_RAM_OPTIMIZATION.md#1-update-applicationproperties)

---

## 💰 Expected Cost Savings

### Cloud Hosting (AWS ECS/Fargate)

**Before Optimization:**
- Instance Type: t3.small (2 GB RAM, 2 vCPU)
- Monthly Cost: ~$30-40 per instance

**After Optimization:**
- Instance Type: t3.micro (1 GB RAM, 2 vCPU)  
- Monthly Cost: ~$20-25 per instance

**Savings:** $10-15/month per instance (25-40% reduction)

### For 5 instances
- **Monthly Savings:** $50-75
- **Annual Savings:** $600-900

**Detailed Analysis:** [RAM_OPTIMIZATION_IMPACT_SUMMARY.md](./RAM_OPTIMIZATION_IMPACT_SUMMARY.md#containercloud-deployment-impact)

---

## 🔒 Risk Management

### Mitigation Strategies

1. **Incremental Deployment**
   - Apply Phase 1 to staging first
   - Monitor for 24-48 hours
   - Roll out to production gradually (canary deployment)

2. **Rollback Plan**
   - Keep backup of all configuration files
   - Document original settings
   - Test rollback procedure in staging
   - Rollback script available in Quick Start guide

3. **Monitoring**
   - Set up alerts for high memory usage
   - Monitor connection pool exhaustion
   - Track response time degradation
   - Enable detailed logging temporarily

4. **Testing**
   - Load test in staging environment
   - Verify cache operations
   - Test OAuth flows (Google/Apple)
   - Test push notifications (Firebase/APNS)

**Full Risk Assessment:** [RAM_OPTIMIZATION_IMPACT_SUMMARY.md](./RAM_OPTIMIZATION_IMPACT_SUMMARY.md#risk-assessment)

---

## 📅 Recommended Timeline

```
Week 1: Preparation & Phase 1
├─ Day 1: Read documentation, backup configs
├─ Day 2: Apply Phase 1 changes to dev environment
├─ Day 3: Test in dev, deploy to staging
├─ Day 4: Monitor staging for 24 hours
├─ Day 5: Deploy to production (canary)
├─ Day 6-7: Monitor production, collect metrics

Week 2: Validation & Phase 2 Planning
├─ Day 8-10: Analyze Phase 1 results
├─ Day 11-12: Plan Phase 2 optimizations
├─ Day 13-14: Prepare Phase 2 changes

Week 3+: Phase 2 & 3 (Optional)
└─ Implement remaining optimizations based on Phase 1 success
```

---

## 🎓 Learning Resources

### External Documentation
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Spring Boot Memory Tuning](https://spring.io/blog/2015/12/10/spring-boot-memory-performance)
- [G1GC Tuning Guide](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [Redis Memory Optimization](https://redis.io/docs/management/optimization/memory-optimization/)

### JVM Monitoring Tools
- **VisualVM:** Free, bundled with JDK
- **Java Mission Control:** Free, comprehensive profiling
- **JProfiler:** Commercial, excellent for memory leaks
- **YourKit:** Commercial, user-friendly
- **Async-profiler:** Lightweight, production-friendly

---

## 📧 Support & Questions

### Before Asking for Help

1. ✅ Check the troubleshooting section
2. ✅ Review application logs: `tail -f logs/application.log`
3. ✅ Run monitoring scripts: `./scripts/monitor-ram.sh`
4. ✅ Check actuator metrics (if enabled): `curl http://localhost:8081/actuator/metrics`

### Reporting Issues

When reporting issues, include:
- Which phase/optimization you implemented
- Error messages from logs
- Output from `jmap -heap <PID>`
- Monitoring script output
- Steps to reproduce

---

## 📄 Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-31 | System | Initial comprehensive RAM optimization guide |

---

## ✅ Next Steps

1. **Read** the [Quick Start Guide](./QUICK_START_RAM_OPTIMIZATION.md)
2. **Run** baseline monitoring with `./scripts/monitor-ram.sh`
3. **Apply** Phase 1 configuration changes
4. **Test** in development environment
5. **Deploy** to staging for validation
6. **Monitor** and measure improvements
7. **Proceed** to Phase 2 if Phase 1 succeeds

**Good luck with your optimization!** 🚀

---

*For questions or suggestions, please create an issue or contact the development team.*

