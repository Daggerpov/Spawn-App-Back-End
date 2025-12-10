#!/bin/bash

# Spawn App - RAM Log Analysis Script
# Analyzes RAM monitoring logs and compares before/after optimization
# Usage: ./analyze-ram-logs.sh <log_file_1> [log_file_2]

set -e

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

if [ $# -eq 0 ]; then
    echo "Usage: $0 <log_file_1> [log_file_2]"
    echo ""
    echo "Examples:"
    echo "  $0 logs/ram-monitor-20251031.log"
    echo "  $0 logs/before.log logs/after.log"
    exit 1
fi

LOG1="$1"
LOG2="${2:-}"

if [ ! -f "$LOG1" ]; then
    echo -e "${RED}Error: Log file '$LOG1' not found${NC}"
    exit 1
fi

# Function to calculate statistics from log file
analyze_log() {
    local logfile=$1
    local label=$2
    
    # Skip header line and calculate stats
    local heap_values=$(tail -n +2 "$logfile" | cut -d',' -f2)
    local metaspace_values=$(tail -n +2 "$logfile" | cut -d',' -f4)
    local thread_values=$(tail -n +2 "$logfile" | cut -d',' -f5)
    local gc_count_values=$(tail -n +2 "$logfile" | cut -d',' -f6)
    
    # Calculate average heap
    local avg_heap=$(echo "$heap_values" | awk '{sum+=$1; count++} END {if(count>0) print sum/count; else print 0}')
    local max_heap=$(echo "$heap_values" | sort -n | tail -1)
    local min_heap=$(echo "$heap_values" | sort -n | head -1)
    
    # Calculate average metaspace
    local avg_metaspace=$(echo "$metaspace_values" | awk '{sum+=$1; count++} END {if(count>0) print sum/count; else print 0}')
    
    # Calculate average threads
    local avg_threads=$(echo "$thread_values" | awk '{sum+=$1; count++} END {if(count>0) print sum/count; else print 0}')
    
    # Get first and last GC count
    local first_gc=$(echo "$gc_count_values" | head -1)
    local last_gc=$(echo "$gc_count_values" | tail -1)
    local gc_diff=$((last_gc - first_gc))
    
    # Count number of samples
    local sample_count=$(echo "$heap_values" | wc -l | tr -d ' ')
    
    # Get time range
    local first_time=$(tail -n +2 "$logfile" | head -1 | cut -d',' -f1)
    local last_time=$(tail -n +2 "$logfile" | tail -1 | cut -d',' -f1)
    
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}Analysis: $label${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "Log File: $logfile"
    echo "Time Range: $first_time to $last_time"
    echo "Samples: $sample_count"
    echo ""
    echo "HEAP MEMORY:"
    printf "  Average:  %8.2f MB\n" "$avg_heap"
    printf "  Minimum:  %8.2f MB\n" "$min_heap"
    printf "  Maximum:  %8.2f MB\n" "$max_heap"
    echo ""
    echo "METASPACE:"
    printf "  Average:  %8.2f MB\n" "$avg_metaspace"
    echo ""
    echo "THREADS:"
    printf "  Average:  %8.0f threads\n" "$avg_threads"
    echo ""
    echo "GARBAGE COLLECTION:"
    printf "  Total GC Events: %d\n" "$gc_diff"
    if [ "$sample_count" -gt 0 ]; then
        local duration_minutes=$(echo "scale=2; $sample_count * 5 / 60" | bc)
        local gc_per_minute=$(echo "scale=2; $gc_diff / $duration_minutes" | bc)
        printf "  GC Frequency: %.2f per minute\n" "$gc_per_minute"
    fi
    echo ""
    
    # Return values for comparison
    echo "$avg_heap|$max_heap|$avg_metaspace|$avg_threads|$gc_diff|$sample_count"
}

# Analyze first log
result1=$(analyze_log "$LOG1" "Log 1")
IFS='|' read -r avg_heap1 max_heap1 avg_meta1 avg_threads1 gc1 samples1 <<< "$result1"

# If second log provided, analyze and compare
if [ -n "$LOG2" ]; then
    if [ ! -f "$LOG2" ]; then
        echo -e "${RED}Error: Log file '$LOG2' not found${NC}"
        exit 1
    fi
    
    result2=$(analyze_log "$LOG2" "Log 2")
    IFS='|' read -r avg_heap2 max_heap2 avg_meta2 avg_threads2 gc2 samples2 <<< "$result2"
    
    # Calculate differences
    heap_diff=$(echo "scale=2; $avg_heap1 - $avg_heap2" | bc)
    heap_pct=$(echo "scale=1; ($heap_diff / $avg_heap1) * 100" | bc)
    
    max_heap_diff=$(echo "scale=2; $max_heap1 - $max_heap2" | bc)
    max_heap_pct=$(echo "scale=1; ($max_heap_diff / $max_heap1) * 100" | bc)
    
    meta_diff=$(echo "scale=2; $avg_meta1 - $avg_meta2" | bc)
    meta_pct=$(echo "scale=1; ($meta_diff / $avg_meta1) * 100" | bc)
    
    threads_diff=$(echo "scale=0; $avg_threads1 - $avg_threads2" | bc)
    threads_pct=$(echo "scale=1; ($threads_diff / $avg_threads1) * 100" | bc)
    
    # Calculate GC frequency change
    duration1=$(echo "scale=2; $samples1 * 5 / 60" | bc)
    duration2=$(echo "scale=2; $samples2 * 5 / 60" | bc)
    gc_freq1=$(echo "scale=2; $gc1 / $duration1" | bc)
    gc_freq2=$(echo "scale=2; $gc2 / $duration2" | bc)
    gc_freq_diff=$(echo "scale=2; $gc_freq1 - $gc_freq2" | bc)
    gc_freq_pct=$(echo "scale=1; ($gc_freq_diff / $gc_freq1) * 100" | bc)
    
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}COMPARISON (Log 1 vs Log 2)${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    
    # Heap comparison
    if (( $(echo "$heap_diff > 0" | bc -l) )); then
        echo -e "${GREEN}✓ Average Heap Memory REDUCED${NC}"
        printf "  Savings: %.2f MB (%.1f%% reduction)\n" "$heap_diff" "$heap_pct"
    elif (( $(echo "$heap_diff < 0" | bc -l) )); then
        echo -e "${RED}✗ Average Heap Memory INCREASED${NC}"
        printf "  Increase: %.2f MB (%.1f%% increase)\n" "${heap_diff#-}" "${heap_pct#-}"
    else
        echo -e "${YELLOW}○ Average Heap Memory UNCHANGED${NC}"
    fi
    echo ""
    
    # Max heap comparison
    if (( $(echo "$max_heap_diff > 0" | bc -l) )); then
        echo -e "${GREEN}✓ Maximum Heap Memory REDUCED${NC}"
        printf "  Savings: %.2f MB (%.1f%% reduction)\n" "$max_heap_diff" "$max_heap_pct"
    elif (( $(echo "$max_heap_diff < 0" | bc -l) )); then
        echo -e "${RED}✗ Maximum Heap Memory INCREASED${NC}"
        printf "  Increase: %.2f MB (%.1f%% increase)\n" "${max_heap_diff#-}" "${max_heap_pct#-}"
    else
        echo -e "${YELLOW}○ Maximum Heap Memory UNCHANGED${NC}"
    fi
    echo ""
    
    # Metaspace comparison
    if (( $(echo "$meta_diff > 0" | bc -l) )); then
        echo -e "${GREEN}✓ Metaspace REDUCED${NC}"
        printf "  Savings: %.2f MB (%.1f%% reduction)\n" "$meta_diff" "$meta_pct"
    elif (( $(echo "$meta_diff < 0" | bc -l) )); then
        echo -e "${YELLOW}○ Metaspace INCREASED${NC}"
        printf "  Increase: %.2f MB (%.1f%% increase)\n" "${meta_diff#-}" "${meta_pct#-}"
    else
        echo -e "${YELLOW}○ Metaspace UNCHANGED${NC}"
    fi
    echo ""
    
    # Thread comparison
    if (( $(echo "$threads_diff > 0" | bc -l) )); then
        echo -e "${GREEN}✓ Thread Count REDUCED${NC}"
        printf "  Savings: %.0f threads (%.1f%% reduction)\n" "$threads_diff" "$threads_pct"
    elif (( $(echo "$threads_diff < 0" | bc -l) )); then
        echo -e "${RED}✗ Thread Count INCREASED${NC}"
        printf "  Increase: %.0f threads (%.1f%% increase)\n" "${threads_diff#-}" "${threads_pct#-}"
    else
        echo -e "${YELLOW}○ Thread Count UNCHANGED${NC}"
    fi
    echo ""
    
    # GC frequency comparison
    if (( $(echo "$gc_freq_diff > 0" | bc -l) )); then
        echo -e "${GREEN}✓ GC Frequency REDUCED${NC}"
        printf "  Before: %.2f GC/min → After: %.2f GC/min\n" "$gc_freq1" "$gc_freq2"
        printf "  Improvement: %.1f%% reduction\n" "$gc_freq_pct"
    elif (( $(echo "$gc_freq_diff < 0" | bc -l) )); then
        echo -e "${RED}✗ GC Frequency INCREASED${NC}"
        printf "  Before: %.2f GC/min → After: %.2f GC/min\n" "$gc_freq1" "$gc_freq2"
        printf "  Change: %.1f%% increase\n" "${gc_freq_pct#-}"
    else
        echo -e "${YELLOW}○ GC Frequency UNCHANGED${NC}"
    fi
    echo ""
    
    # Overall summary
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}SUMMARY${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    
    total_savings=$(echo "scale=2; $heap_diff + $meta_diff" | bc)
    thread_ram_savings=$(echo "scale=2; $threads_diff * 1" | bc)  # Assume 1MB per thread
    estimated_total=$(echo "scale=2; $total_savings + $thread_ram_savings" | bc)
    
    echo "Estimated Total RAM Savings:"
    printf "  Heap Memory:        %8.2f MB\n" "$heap_diff"
    printf "  Metaspace:          %8.2f MB\n" "$meta_diff"
    printf "  Thread Stacks:      %8.2f MB (estimated)\n" "$thread_ram_savings"
    echo "  ----------------------------------------"
    printf "  TOTAL:              %8.2f MB\n" "$estimated_total"
    echo ""
    
    if (( $(echo "$estimated_total > 200" | bc -l) )); then
        echo -e "${GREEN}🎉 Excellent! Significant RAM reduction achieved!${NC}"
    elif (( $(echo "$estimated_total > 100" | bc -l) )); then
        echo -e "${GREEN}✓ Good! Notable RAM reduction achieved.${NC}"
    elif (( $(echo "$estimated_total > 50" | bc -l) )); then
        echo -e "${YELLOW}○ Moderate RAM reduction. Consider Phase 2 optimizations.${NC}"
    elif (( $(echo "$estimated_total > 0" | bc -l) )); then
        echo -e "${YELLOW}○ Minor RAM reduction. Review implementation.${NC}"
    else
        echo -e "${RED}⚠ No RAM reduction or increase. Please review changes.${NC}"
    fi
    echo ""
fi

# Generate visualization hint
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}VISUALIZATION${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "To visualize these logs, you can:"
echo ""
echo "1. Import CSV into Excel/Google Sheets"
echo "2. Use gnuplot (if installed):"
echo "   gnuplot -e \"set terminal png; set output 'graph.png'; plot '$LOG1' using 2 with lines title 'Heap MB'\""
echo ""
echo "3. Use Python pandas:"
echo "   python3 -c \"import pandas as pd; import matplotlib.pyplot as plt; df=pd.read_csv('$LOG1'); df.plot(x='timestamp', y='heap_used_mb'); plt.savefig('graph.png')\""
echo ""

