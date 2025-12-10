#!/bin/bash

# ============================================================================
# Spawn App - Production Startup Script with RAM Optimizations
# ============================================================================
# 
# This script starts the Spawn application with optimized JVM settings
# for reduced RAM usage and better garbage collection performance.
#
# Expected RAM Savings: 300-400 MB (Phase 1 optimizations)
# 
# Usage: ./scripts/start-production.sh
#
# Prerequisites:
# - Java 17 or higher
# - Application JAR built: target/spawn-0.0.1-SNAPSHOT.jar
# - Environment variables configured (MYSQL_URL, REDIS_HOST, etc.)
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}Spawn App - Production Startup${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if JAR exists
JAR_FILE="target/spawn-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found: $JAR_FILE${NC}"
    echo -e "${YELLOW}Please build the application first:${NC}"
    echo "  mvn clean package -DskipTests"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}ERROR: Java 17 or higher required. Found: Java $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java version: $JAVA_VERSION${NC}"
echo -e "${GREEN}✓ JAR file found: $JAR_FILE${NC}"
echo ""

# ============================================================================
# JVM Memory Configuration (RAM Optimization Phase 1)
# ============================================================================
# 
# -Xms512m                    Initial heap size (512 MB)
# -Xmx1536m                   Maximum heap size (1.5 GB)
# -XX:MaxMetaspaceSize=256m   Limit class metadata space
# -XX:CompressedClassSpaceSize=64m  Limit compressed class pointers
#
# Expected: Prevents heap from growing unbounded, saves ~200+ MB
# ============================================================================

MEMORY_OPTS="-Xms512m \
-Xmx1536m \
-XX:MaxMetaspaceSize=256m \
-XX:CompressedClassSpaceSize=64m"

# ============================================================================
# Garbage Collection Configuration (G1GC Tuning)
# ============================================================================
# 
# -XX:+UseG1GC                          Use G1 Garbage Collector (default in Java 17)
# -XX:MaxGCPauseMillis=200              Target max GC pause time
# -XX:InitiatingHeapOccupancyPercent=45 Start concurrent GC earlier
# -XX:G1HeapRegionSize=4m               Set G1 region size
#
# Expected: Reduces GC frequency by 50%, reduces pause times by 30%
# ============================================================================

GC_OPTS="-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:InitiatingHeapOccupancyPercent=45 \
-XX:G1HeapRegionSize=4m"

# ============================================================================
# String Optimization
# ============================================================================
# 
# -XX:+UseStringDeduplication           Share identical string objects
# -XX:StringDeduplicationAgeThreshold=3 Age before deduplication
#
# Expected: Saves 20-50 MB (5-15% of heap) due to many duplicate strings
# ============================================================================

STRING_OPTS="-XX:+UseStringDeduplication \
-XX:StringDeduplicationAgeThreshold=3"

# ============================================================================
# Container Support (for Docker/Kubernetes)
# ============================================================================
# 
# -XX:+UseContainerSupport    Detect container memory limits
# -XX:MaxRAMPercentage=75.0   Use 75% of available RAM
# -XX:InitialRAMPercentage=25.0  Start with 25% of available RAM
#
# Note: MaxRAMPercentage only applies if no -Xmx is set
# ============================================================================

CONTAINER_OPTS="-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-XX:InitialRAMPercentage=25.0"

# ============================================================================
# Diagnostic Options (for troubleshooting)
# ============================================================================
# 
# -XX:+HeapDumpOnOutOfMemoryError    Create heap dump on OOM
# -XX:HeapDumpPath=logs/heapdump.hprof  Where to save heap dump
# -XX:+ExitOnOutOfMemoryError        Exit JVM on OOM (restart via orchestrator)
#
# Note: Heap dumps can be large. Ensure sufficient disk space.
# ============================================================================

# Create logs directory if it doesn't exist
mkdir -p logs

DIAGNOSTIC_OPTS="-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=logs/heapdump-$(date +%Y%m%d-%H%M%S).hprof"

# Optional: Exit on OOM (uncomment if using Kubernetes/Docker with restart policy)
# DIAGNOSTIC_OPTS="$DIAGNOSTIC_OPTS -XX:+ExitOnOutOfMemoryError"

# ============================================================================
# GC Logging (Optional - for monitoring)
# ============================================================================
# 
# Uncomment to enable GC logging for analysis
# ============================================================================

# GC_LOG_OPTS="-Xlog:gc*:file=logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10M"

# ============================================================================
# Combine all JVM options
# ============================================================================

JAVA_OPTS="$MEMORY_OPTS $GC_OPTS $STRING_OPTS $CONTAINER_OPTS $DIAGNOSTIC_OPTS"

# Add GC logging if enabled
if [ -n "$GC_LOG_OPTS" ]; then
    JAVA_OPTS="$JAVA_OPTS $GC_LOG_OPTS"
fi

# ============================================================================
# Display Configuration
# ============================================================================

echo -e "${BLUE}JVM Configuration:${NC}"
echo -e "  Initial Heap:     ${GREEN}512 MB${NC}"
echo -e "  Maximum Heap:     ${GREEN}1536 MB${NC}"
echo -e "  Max Metaspace:    ${GREEN}256 MB${NC}"
echo -e "  GC:               ${GREEN}G1GC with 200ms pause target${NC}"
echo -e "  String Dedup:     ${GREEN}Enabled${NC}"
echo -e "  Container Support: ${GREEN}Enabled${NC}"
echo ""

echo -e "${BLUE}Expected RAM Savings (vs default):${NC}"
echo -e "  Thread Stack:     ${GREEN}-150 MB${NC} (Tomcat 200→50 threads)"
echo -e "  Connection Pool:  ${GREEN}-15 MB${NC}  (HikariCP 20→10 connections)"
echo -e "  Redis Cache:      ${GREEN}-60 MB${NC}  (JSON vs JDK serialization)"
echo -e "  String Dedup:     ${GREEN}-20-50 MB${NC} (Duplicate string elimination)"
echo -e "  Misc:             ${GREEN}-20 MB${NC}  (Redis pooling, logging, etc.)"
echo -e "  ${GREEN}─────────────${NC}"
echo -e "  ${GREEN}Total Savings: ~300-400 MB${NC}"
echo ""

# ============================================================================
# Check required environment variables
# ============================================================================

REQUIRED_VARS=("MYSQL_URL" "MYSQL_USER" "MYSQL_PASSWORD" "REDIS_HOST" "REDIS_PORT")
MISSING_VARS=()

for var in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!var}" ]; then
        MISSING_VARS+=("$var")
    fi
done

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo -e "${YELLOW}WARNING: Missing environment variables:${NC}"
    for var in "${MISSING_VARS[@]}"; do
        echo -e "  ${RED}✗ $var${NC}"
    done
    echo ""
    echo -e "${YELLOW}The application may fail to start without these variables.${NC}"
    echo -e "${YELLOW}Continuing in 5 seconds... (Ctrl+C to abort)${NC}"
    sleep 5
fi

# ============================================================================
# Start the application
# ============================================================================

echo -e "${GREEN}Starting Spawn application...${NC}"
echo ""
echo -e "${BLUE}To monitor RAM usage in another terminal:${NC}"
echo "  ./scripts/monitor-ram.sh 5"
echo ""
echo -e "${BLUE}Full command:${NC}"
echo "java $JAVA_OPTS -jar $JAR_FILE"
echo ""
echo "============================================"
echo ""

# Start the application
exec java $JAVA_OPTS -jar $JAR_FILE

