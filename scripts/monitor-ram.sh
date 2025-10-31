#!/bin/bash

# Spawn App - RAM Monitoring Script
# Monitors JVM memory, threads, GC, and connection pools
# Usage: ./monitor-ram.sh [interval_seconds]

set -e

INTERVAL=${1:-5}  # Default 5 seconds
LOG_FILE="logs/ram-monitor-$(date +%Y%m%d-%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Ensure logs directory exists
mkdir -p logs

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Spawn App - RAM Monitoring${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Interval: ${INTERVAL} seconds"
echo "Log file: ${LOG_FILE}"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo ""

# Write CSV header
echo "timestamp,heap_used_mb,heap_max_mb,metaspace_mb,threads,gc_count,gc_time_ms,active_db_conn" > "$LOG_FILE"

# Function to get JVM process ID
get_jvm_pid() {
    jps | grep -i spawn | awk '{print $1}'
}

# Function to format bytes to MB
bytes_to_mb() {
    echo "scale=2; $1 / 1024 / 1024" | bc
}

# Function to get heap memory info
get_heap_info() {
    local pid=$1
    jmap -heap "$pid" 2>/dev/null | grep -A 5 "Heap Configuration" | grep -E "MaxHeapSize|MinHeapSize"
}

# Main monitoring loop
monitor_iteration=0
while true; do
    PID=$(get_jvm_pid)
    
    if [ -z "$PID" ]; then
        echo -e "${RED}ERROR: Spawn application not running!${NC}"
        sleep "$INTERVAL"
        continue
    fi
    
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Get memory info using jstat
    JSTAT_OUTPUT=$(jstat -gc "$PID" 2>/dev/null)
    
    if [ -z "$JSTAT_OUTPUT" ]; then
        echo -e "${RED}ERROR: Cannot get jstat output${NC}"
        sleep "$INTERVAL"
        continue
    fi
    
    # Parse jstat output
    # S0C S1C S0U S1U EC EU OC OU MC MU CCSC CCSU YGC YGCT FGC FGCT CGC CGCT GCT
    read -r S0C S1C S0U S1U EC EU OC OU MC MU CCSC CCSU YGC YGCT FGC FGCT CGC CGCT GCT <<< "$(echo "$JSTAT_OUTPUT" | tail -1)"
    
    # Calculate heap usage (Young + Old generation in KB)
    HEAP_USED_KB=$(echo "scale=2; $S0U + $S1U + $EU + $OU" | bc)
    HEAP_MAX_KB=$(echo "scale=2; $S0C + $S1C + $EC + $OC" | bc)
    HEAP_USED_MB=$(echo "scale=2; $HEAP_USED_KB / 1024" | bc)
    HEAP_MAX_MB=$(echo "scale=2; $HEAP_MAX_KB / 1024" | bc)
    
    # Metaspace
    METASPACE_MB=$(echo "scale=2; $MU / 1024" | bc)
    
    # Total GC count and time
    TOTAL_GC_COUNT=$(echo "$YGC + $FGC" | bc)
    TOTAL_GC_TIME_MS=$(echo "scale=2; $GCT * 1000" | bc)
    
    # Get thread count
    THREAD_COUNT=$(jstack "$PID" 2>/dev/null | grep -c "^\"" || echo "N/A")
    
    # Try to get DB connection count from actuator (if available)
    ACTIVE_DB_CONN=$(curl -s http://localhost:8081/actuator/metrics/hikaricp.connections.active 2>/dev/null | grep -o '"value":[0-9]*' | grep -o '[0-9]*' || echo "N/A")
    
    # Calculate heap usage percentage
    HEAP_PCT=$(echo "scale=1; $HEAP_USED_MB * 100 / $HEAP_MAX_MB" | bc)
    
    # Clear screen every 10 iterations for better readability
    if [ $((monitor_iteration % 10)) -eq 0 ]; then
        clear
        echo -e "${BLUE}========================================${NC}"
        echo -e "${BLUE}Spawn App - RAM Monitoring${NC}"
        echo -e "${BLUE}========================================${NC}"
        echo ""
        echo "PID: $PID | Interval: ${INTERVAL}s | Log: ${LOG_FILE}"
        echo ""
        printf "%-20s | %-15s | %-15s | %-10s | %-10s | %-15s\n" \
            "TIME" "HEAP USED" "HEAP MAX" "META MB" "THREADS" "GC COUNT"
        echo "----------------------------------------------------------------------------------------------------"
    fi
    
    # Color-code heap usage
    HEAP_COLOR=$GREEN
    if (( $(echo "$HEAP_PCT > 70" | bc -l) )); then
        HEAP_COLOR=$YELLOW
    fi
    if (( $(echo "$HEAP_PCT > 85" | bc -l) )); then
        HEAP_COLOR=$RED
    fi
    
    # Display current stats
    printf "%-20s | ${HEAP_COLOR}%-15s${NC} | %-15s | %-10s | %-10s | %-15s\n" \
        "$TIMESTAMP" \
        "${HEAP_USED_MB}MB (${HEAP_PCT}%)" \
        "${HEAP_MAX_MB}MB" \
        "${METASPACE_MB}MB" \
        "$THREAD_COUNT" \
        "$TOTAL_GC_COUNT"
    
    # Log to CSV
    echo "$TIMESTAMP,$HEAP_USED_MB,$HEAP_MAX_MB,$METASPACE_MB,$THREAD_COUNT,$TOTAL_GC_COUNT,$TOTAL_GC_TIME_MS,$ACTIVE_DB_CONN" >> "$LOG_FILE"
    
    # Show detailed GC info every 5 iterations
    if [ $((monitor_iteration % 5)) -eq 0 ] && [ $monitor_iteration -gt 0 ]; then
        echo ""
        echo -e "${YELLOW}GC Details:${NC}"
        echo "  Young GC: $YGC collections, ${YGCT}s total"
        echo "  Full GC:  $FGC collections, ${FGCT}s total"
        if [ "$ACTIVE_DB_CONN" != "N/A" ]; then
            echo "  Active DB Connections: $ACTIVE_DB_CONN"
        fi
        echo ""
    fi
    
    monitor_iteration=$((monitor_iteration + 1))
    sleep "$INTERVAL"
done

